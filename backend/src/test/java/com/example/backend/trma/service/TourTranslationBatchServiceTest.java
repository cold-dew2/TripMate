package com.example.backend.trma.service;

import com.example.backend.trma.mapper.TourTranslationMapper;
import com.example.backend.trma.util.AiErrorUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("[번역 배치] TourTranslationBatchService")
class TourTranslationBatchServiceTest {

    @Mock TourTranslationMapper mapper;
    @Mock TourListService tourListService;
    TourTranslationService translationService;
    TourTranslationBatchService service;

    @BeforeEach
    void setUp() {
        AiErrorUtil.clearQuotaBlock();
        translationService = new TourTranslationService(mapper);
        service = new TourTranslationBatchService(mapper, translationService, tourListService);
    }

    @AfterEach
    void tearDown() {
        AiErrorUtil.clearQuotaBlock();
    }

    static Map<String, String> row(String id, String src) {
        return Map.of("tourId", id, "source", src);
    }

    @Test
    @DisplayName("누락 주소를 배치로 번역해 원문과 함께 AI 방식으로 저장하고, 실패한 번역은 저장하지 않는다")
    void translatesAndSaves() {
        when(mapper.missingTranslations("ROAD_ADDR", "ROAD_ADDR_EN", 2, "")).thenReturn(List.of(row("T1", "서울 종로구"), row("T2", "부산 중구")));
        when(mapper.missingTranslations("ROAD_ADDR", "ROAD_ADDR_EN", 2, "T2")).thenReturn(List.of());
        when(tourListService.translateFreeTexts(any(), eq("en"))).thenReturn(Map.of("T1", "Jongno-gu, Seoul", "T2", "부산 중구"));

        var r = service.translateMissing("ROAD_ADDR", "en", 2, 10);

        assertThat(r.saved()).isEqualTo(1);
        assertThat(r.failed()).isEqualTo(1);
        verify(mapper).upsertTranslation("T1", "ROAD_ADDR", "en", "AI", "서울 종로구", "Jongno-gu, Seoul");
        verify(mapper, never()).upsertTranslation(eq("T2"), any(), any(), any(), any(), any());
        assertThat(translationService.tryClaim("T1", "ROAD_ADDR", "en")).as("잠금 해제").isTrue();
    }

    @Test
    @DisplayName("Gemini 한도/크레딧 소진 상태면 AI를 부르지 않고 바로 멈춘다(남은 항목은 재처리 대상으로 그대로)")
    void stopsOnQuota() {
        AiErrorUtil.markQuotaBlocked(60_000);

        var r = service.translateMissing("ROAD_ADDR", "en", 20, 5);

        assertThat(r.attempted()).isZero();
        assertThat(r.stopReason()).contains("한도");
        verifyNoInteractions(tourListService);
        verify(mapper, never()).upsertTranslation(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("배치 도중 한도에 걸리면 그 배치까지만 처리하고 멈춘다")
    void stopsWhenQuotaHitMidway() {
        when(mapper.missingTranslations(eq("TOUR_CONTENT"), eq("TOUR_CONTENT_JA"), eq(1), any())).thenReturn(List.of(row("T1", "설명")));
        when(tourListService.translateFreeTexts(any(), eq("ja"))).thenAnswer(inv -> {
            AiErrorUtil.markQuotaBlocked(60_000); // 이번 호출에서 402/429를 받았다고 가정
            return Map.of();
        });

        var r = service.translateMissing("TOUR_CONTENT", "ja", 1, 10);

        assertThat(r.batches()).isEqualTo(1);
        assertThat(r.saved()).isZero();
        assertThat(r.stopReason()).contains("한도");
    }

    @Test
    @DisplayName("연속 2개 배치에서 저장이 0건이면(한도 외 AI 오류) 중단한다")
    void stopsOnRepeatedFailure() {
        when(mapper.missingTranslations(eq("ROAD_ADDR"), eq("ROAD_ADDR_EN"), eq(1), any()))
                .thenReturn(List.of(row("T1", "서울")), List.of(row("T2", "부산")), List.of(row("T3", "대구")));
        when(tourListService.translateFreeTexts(any(), any())).thenReturn(Map.of());

        var r = service.translateMissing("ROAD_ADDR", "en", 1, 10);

        assertThat(r.batches()).isEqualTo(2);
        assertThat(r.stopReason()).contains("연속");
    }

    @Test
    @DisplayName("다른 요청이 번역 중인 항목은 건너뛴다(중복 AI 호출 방지)")
    void skipsInFlight() {
        when(mapper.missingTranslations(eq("ROAD_ADDR"), eq("ROAD_ADDR_EN"), eq(5), eq(""))).thenReturn(List.of(row("T1", "서울")));
        when(mapper.missingTranslations(eq("ROAD_ADDR"), eq("ROAD_ADDR_EN"), eq(5), eq("T1"))).thenReturn(List.of());
        translationService.tryClaim("T1", "ROAD_ADDR", "en");

        var r = service.translateMissing("ROAD_ADDR", "en", 5, 3);

        assertThat(r.skippedInFlight()).isEqualTo(1);
        verifyNoInteractions(tourListService);
    }

    @Test
    @DisplayName("이름은 기존 이름 번역 기능(translateTourNames)을 쓰고 그 안에서 저장되므로 여기서 다시 저장하지 않는다")
    void namesUseNameTranslator() {
        when(mapper.missingTranslations("TOUR_NM", "TOUR_NM_EN", 20, "")).thenReturn(List.of(row("T1", "경복궁")));
        when(mapper.missingTranslations("TOUR_NM", "TOUR_NM_EN", 20, "T1")).thenReturn(List.of());
        when(tourListService.translateTourNames(List.of("T1"), "en")).thenReturn(Map.of("T1", "Gyeongbokgung Palace"));

        var r = service.translateMissing("TOUR_NM", "en", 20, 5);

        assertThat(r.saved()).isEqualTo(1);
        verify(mapper, never()).upsertTranslation(any(), any(), any(), any(), any(), any());
        // [회귀] 배치가 이름 잠금을 먼저 잡으면 안쪽 이름 번역이 전부 건너뛰어졌다
        assertThat(translationService.tryClaim("T1", "TOUR_NM", "en")).isTrue();
    }

    @Test
    @DisplayName("허용되지 않은 항목/언어는 거부한다(SQL 컬럼 화이트리스트)")
    void rejectsUnknown() {
        assertThatThrownBy(() -> service.translateMissing("USER_PW", "en", 1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.translateMissing("TOUR_NM", "ko", 1, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    // ───────────── 외부 엔진(Claude) 번역 가져오기 ─────────────

    static TourTranslationBatchService.ImportItem item(String id, String field, String lang, String src, String text) {
        return new TourTranslationBatchService.ImportItem(id, field, lang, src, text);
    }

    static Map<String, String> state(String source, String value) {
        Map<String, String> m = new java.util.HashMap<>();
        m.put("source", source);
        m.put("value", value);
        return m;
    }

    @Test
    @DisplayName("[가져오기] 정상 번역은 원문과 함께 AI 방식으로 저장하고 최종값은 관광지 묶음으로 한 번 재계산한다")
    void importSaves() {
        when(mapper.currentState("TOUR_NM", "TOUR_NM_EN", "T1")).thenReturn(state("경복궁", null));
        when(mapper.currentState("ROAD_ADDR", "ROAD_ADDR_JA", "T1")).thenReturn(state("서울 종로구", null));

        var r = service.importTranslations(List.of(
                item("T1", "TOUR_NM", "en", "경복궁", " Gyeongbokgung Palace "),
                item("T1", "ROAD_ADDR", "ja", "서울 종로구", "ソウル 鍾路区")));

        assertThat(r.saved()).isEqualTo(2);
        verify(mapper).upsertTranslation("T1", "TOUR_NM", "en", "AI", "경복궁", "Gyeongbokgung Palace");
        verify(mapper).upsertTranslation("T1", "ROAD_ADDR", "ja", "AI", "서울 종로구", "ソウル 鍾路区");
        verify(mapper, times(1)).resolveTourColumns(List.of("T1"));
    }

    @Test
    @DisplayName("[가져오기] 원문이 바뀌었거나, 이미 유효 번역(수동/공식 등)이 있거나, 실패 번역이면 저장하지 않는다")
    void importGuards() {
        when(mapper.currentState("TOUR_NM", "TOUR_NM_EN", "T1")).thenReturn(state("경복궁(변경)", null));
        when(mapper.currentState("TOUR_NM", "TOUR_NM_EN", "T2")).thenReturn(state("남산", "Namsan (manual)"));

        var r = service.importTranslations(List.of(
                item("T1", "TOUR_NM", "en", "경복궁", "Gyeongbokgung"),        // 원문 변경
                item("T2", "TOUR_NM", "en", "남산", "Namsan"),                // 이미 유효 번역 → 수동 보존
                item("T3", "ROAD_ADDR", "en", "서울 중구", "Seoul 중구"),       // 한글 잔존
                item("T4", "TOUR_NM", "en", "부산", "Пусан"),                  // 키릴 문자
                item("T5", "USER_PW", "en", "x", "y")));                       // 허용되지 않은 항목

        assertThat(r.saved()).isZero();
        assertThat(r.changed()).isEqualTo(1);
        assertThat(r.already()).isEqualTo(1);
        assertThat(r.invalid()).isEqualTo(3);
        verify(mapper, never()).upsertTranslation(any(), any(), any(), any(), any(), any());
        verify(mapper, never()).resolveTourColumns(any());
    }

    @Test
    @DisplayName("[가져오기] 저장 실패는 실패로 집계하고(성공 처리 안 함) 나머지는 계속 저장한다")
    void importSaveFailure() {
        when(mapper.currentState(eq("TOUR_NM"), eq("TOUR_NM_EN"), any())).thenReturn(state("경복궁", null));
        when(mapper.upsertTranslation(eq("T1"), any(), any(), any(), any(), any())).thenThrow(new RuntimeException("db"));

        var r = service.importTranslations(List.of(
                item("T1", "TOUR_NM", "en", "경복궁", "Gyeongbokgung"),
                item("T2", "TOUR_NM", "en", "경복궁", "Gyeongbokgung")));

        assertThat(r.failed()).isEqualTo(1);
        assertThat(r.saved()).isEqualTo(1);
        assertThat(translationService.tryClaim("T1", "TOUR_NM", "en")).as("실패해도 잠금 해제").isTrue();
    }

    static TourTranslationBatchService.ReplaceItem replace(String id, String src, String oldText, String text) {
        return new TourTranslationBatchService.ReplaceItem(id, "TOUR_CONTENT", "en", src, oldText, text);
    }

    static Map<String, String> best(String type, String text) {
        return Map.of("transType", type, "transText", text);
    }

    @Test
    @DisplayName("[교체] 표시 중인 값이 같은 내용의 AI 번역이면 AI 행을 새 번역으로 바꾸고 최종값을 재계산한다")
    void replaceAi() {
        when(mapper.currentState("TOUR_CONTENT", "TOUR_CONTENT_EN", "T1")).thenReturn(state("동락축제 소개", "Dongnak Festival"));
        when(mapper.bestTranslation("TOUR_CONTENT", "en", "T1")).thenReturn(best("AI", "Dongnak Festival"));

        var r = service.replaceAiTranslations(List.of(replace("T1", "동락축제 소개", "Dongnak Festival", "Dongrak Festival")));

        assertThat(r.replaced()).isEqualTo(1);
        verify(mapper).upsertTranslation("T1", "TOUR_CONTENT", "en", "AI", "동락축제 소개", "Dongrak Festival");
        verify(mapper, times(1)).resolveTourColumns(List.of("T1"));
        assertThat(translationService.tryClaim("T1", "TOUR_CONTENT", "en")).as("잠금 해제").isTrue();
    }

    @Test
    @DisplayName("[교체] 수동/공식/기존 번역이 표시 중이거나, 원문·현재값이 바뀌었거나, 실패 번역이면 바꾸지 않는다")
    void replaceGuards() {
        when(mapper.currentState(eq("TOUR_CONTENT"), eq("TOUR_CONTENT_EN"), any())).thenReturn(state("소개", "x"));
        when(mapper.currentState("TOUR_CONTENT", "TOUR_CONTENT_EN", "T5")).thenReturn(state("소개(변경)", "x"));
        when(mapper.bestTranslation("TOUR_CONTENT", "en", "T1")).thenReturn(best("TOURAPI", "Official"));
        when(mapper.bestTranslation("TOUR_CONTENT", "en", "T2")).thenReturn(best("MANUAL", "Manual"));
        when(mapper.bestTranslation("TOUR_CONTENT", "en", "T3")).thenReturn(best("AI", "Newer AI text"));

        var r = service.replaceAiTranslations(List.of(
                replace("T1", "소개", "Official", "New"),     // 공식 번역 표시 중 → 보존
                replace("T2", "소개", "Manual", "New"),       // 수동 번역 표시 중 → 보존
                replace("T3", "소개", "Old AI text", "New"),  // 그 사이 AI 값이 바뀜
                replace("T4", "소개", "Old", "  "),           // 빈 번역(실패 번역)
                replace("T5", "소개", "Old", "New")));        // 원문 변경

        assertThat(r.replaced()).isZero();
        assertThat(r.notAi()).isEqualTo(2);
        assertThat(r.changed()).isEqualTo(2);
        assertThat(r.invalid()).isEqualTo(1);
        verify(mapper, never()).upsertTranslation(any(), any(), any(), any(), any(), any());
        verify(mapper, never()).resolveTourColumns(any());
    }
}
