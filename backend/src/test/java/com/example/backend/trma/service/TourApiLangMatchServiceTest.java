package com.example.backend.trma.service;

import com.example.backend.trma.mapper.TourTranslationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("[TourAPI 다국어 매칭] TourApiLangMatchService")
class TourApiLangMatchServiceTest {

    @Mock TourTranslationMapper mapper;
    TourApiLangMatchService service;

    @BeforeEach
    void setUp() {
        service = new TourApiLangMatchService(mapper);
    }

    static Map<String, Object> ko(String id, String nm, String road, String detail, String x, String y) {
        Map<String, Object> m = new HashMap<>();
        m.put("tourId", id); m.put("tourNm", nm); m.put("roadAddr", road); m.put("detailAddr", detail);
        m.put("mapX", x); m.put("mapY", y);
        return m;
    }

    static Map<String, Object> lang(String id, String title, String addr1, String addr2, String x, String y) {
        Map<String, Object> m = new HashMap<>();
        m.put("contentId", id); m.put("title", title); m.put("addr1", addr1); m.put("addr2", addr2);
        m.put("mapX", x); m.put("mapY", y);
        return m;
    }

    @Test
    @DisplayName("괄호 속 국문명 추출 (영문/일문 괄호, 중첩 괄호)")
    void extractKoreanName() {
        assertThat(TourApiLangMatchService.extractKoreanName("Biyangdo Island (비양도)")).isEqualTo("비양도");
        assertThat(TourApiLangMatchService.extractKoreanName("釜山花火祭り（부산불꽃축제）")).isEqualTo("부산불꽃축제");
        assertThat(TourApiLangMatchService.extractKoreanName("山房山（済州）（산방산（제주））")).isEqualTo("산방산（제주）");
        assertThat(TourApiLangMatchService.extractKoreanName("Lotte World")).isNull();
        assertThat(TourApiLangMatchService.extractKoreanName("Seoul (Jongno)")).isNull();
    }

    @Test
    @DisplayName("공식 표기에서 국문명 괄호를 뗀다")
    void stripKoreanName() {
        assertThat(TourApiLangMatchService.stripKoreanName("Biyangdo Island (비양도)")).isEqualTo("Biyangdo Island");
        assertThat(TourApiLangMatchService.stripKoreanName("山房山（済州）（산방산（제주））")).isEqualTo("山房山（済州）");
    }

    @Test
    @DisplayName("좌표(약 100m)와 국문명이 모두 같을 때만 연결하고, 공식 이름/주소를 TOURAPI 번역으로 저장한다")
    void matchesOnCoordAndName() {
        when(mapper.koreanToursForMatch()).thenReturn(List.of(
                ko("126508", "경복궁", "서울특별시 종로구 사직로 161", "", "126.9767218661", "37.5760307000")));
        when(mapper.langContentsForMatch("en")).thenReturn(List.of(
                lang("264337", "Gyeongbokgung Palace (경복궁)", "161, Sajik-ro, Jongno-gu, Seoul", "", "126.9768", "37.5762")));

        TourApiLangMatchService.MatchResult r = service.match("en");

        assertThat(r.matched()).isEqualTo(1);
        verify(mapper).upsertLangMap("126508", "en", "264337", "COORD_NAME");
        verify(mapper).upsertTranslation("126508", "TOUR_NM", "en", "TOURAPI", "경복궁", "Gyeongbokgung Palace");
        verify(mapper).upsertTranslation("126508", "ROAD_ADDR", "en", "TOURAPI", "서울특별시 종로구 사직로 161", "161, Sajik-ro, Jongno-gu, Seoul");
        // 국문 상세주소가 없으면 저장하지 않는다(원문 해시 불가)
        verify(mapper, never()).upsertTranslation(any(), eq("DETAIL_ADDR"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("[회귀] 좌표가 같아도 이름이 다르면(코엑스 아쿠아리움 ↔ 코엑스아티움) 연결하지 않는다")
    void sameCoordDifferentName() {
        when(mapper.koreanToursForMatch()).thenReturn(List.of(
                ko("T1", "코엑스아쿠아리움", "서울 강남구 영동대로 513", "", "127.0590", "37.5130")));
        when(mapper.langContentsForMatch("en")).thenReturn(List.of(
                lang("E1", "COEX Artium (코엑스아티움)", "513 Yeongdong-daero", "", "127.0591", "37.5131")));

        assertThat(service.match("en").matched()).isZero();
        verify(mapper, never()).upsertLangMap(any(), any(), any(), any());
        verify(mapper, never()).upsertTranslation(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("이름이 같아도 좌표가 멀면(동명이소) 연결하지 않는다")
    void sameNameFarAway() {
        when(mapper.koreanToursForMatch()).thenReturn(List.of(ko("T1", "비양도", "제주", "", "126.2", "33.4")));
        when(mapper.langContentsForMatch("ja")).thenReturn(List.of(lang("J1", "飛揚島（비양도）", "済州", "", "127.5", "34.0")));

        assertThat(service.match("ja").matched()).isZero();
    }

    @Test
    @DisplayName("[회귀] 반올림 경계에 걸린 99m 거리의 같은 장소도 연결한다 (서귀포매일올레시장)")
    void boundaryCoordinates() {
        when(mapper.koreanToursForMatch()).thenReturn(List.of(ko("T1", "서귀포매일올레시장", "제주 서귀포시", "", "126.5644", "33.2499")));
        when(mapper.langContentsForMatch("en")).thenReturn(List.of(
                lang("E1", "Seogwipo Maeil Olle Market (서귀포매일올레시장)", "Seogwipo-si", "", "126.5635", "33.2506")));

        assertThat(service.match("en").matched()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 이름 후보가 1km 안에 둘 이상이면(체인점 등) 모호하므로 연결하지 않는다")
    void ambiguousCandidatesSkipped() {
        when(mapper.koreanToursForMatch()).thenReturn(List.of(ko("T1", "스타벅스", "서울", "", "127.000", "37.500")));
        when(mapper.langContentsForMatch("en")).thenReturn(List.of(
                lang("E1", "Starbucks (스타벅스)", "a", "", "127.001", "37.501"),
                lang("E2", "Starbucks (스타벅스)", "b", "", "127.004", "37.503")));

        assertThat(service.match("en").matched()).isZero();
    }

    @Test
    @DisplayName("좌표 값이 \"null\" 같은 문자열이면 건너뛰고 죽지 않는다")
    void badCoordinates() {
        when(mapper.koreanToursForMatch()).thenReturn(List.of(ko("T1", "비양도", "제주", "", "null", "")));
        when(mapper.langContentsForMatch("en")).thenReturn(List.of(lang("E1", "Biyangdo (비양도)", "Jeju", "", "null", "33.4")));

        assertThat(service.match("en").matched()).isZero();
    }

    @Test
    @DisplayName("공백 차이는 무시하고 이름을 비교한다")
    void whitespaceInsensitive() {
        when(mapper.koreanToursForMatch()).thenReturn(List.of(ko("T1", "부산 불꽃축제", "부산 수영구", "", "129.118", "35.153")));
        when(mapper.langContentsForMatch("ja")).thenReturn(List.of(lang("J1", "釜山花火祭り（부산불꽃축제）", "釜山広域市 水営区", "", "129.1181", "35.1532")));

        assertThat(service.match("ja").matched()).isEqualTo(1);
        verify(mapper).upsertTranslation("T1", "TOUR_NM", "ja", "TOURAPI", "부산 불꽃축제", "釜山花火祭り");
    }

    @Test
    @DisplayName("공식 값에 한글이 남아 있으면(공식 데이터 미번역) 저장하지 않는다")
    void officialWithHangulSkipped() {
        when(mapper.koreanToursForMatch()).thenReturn(List.of(ko("T1", "경복궁", "서울 종로구", "", "126.97", "37.57")));
        when(mapper.langContentsForMatch("en")).thenReturn(List.of(lang("E1", "Gyeongbokgung (경복궁)", "서울 종로구", "", "126.97", "37.57")));

        service.match("en");

        verify(mapper, never()).upsertTranslation(any(), eq("ROAD_ADDR"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("한 건 저장 실패가 전체 매칭을 멈추지 않는다")
    void oneFailureDoesNotStop() {
        when(mapper.koreanToursForMatch()).thenReturn(List.of(
                ko("T1", "경복궁", "서울 종로구", "", "126.97", "37.57"),
                ko("T2", "비양도", "제주 한림읍", "", "126.2", "33.4")));
        when(mapper.langContentsForMatch("en")).thenReturn(List.of(
                lang("E1", "Gyeongbokgung (경복궁)", "Jongno-gu", "", "126.97", "37.57"),
                lang("E2", "Biyangdo (비양도)", "Hallim-eup", "", "126.2", "33.4")));
        when(mapper.upsertLangMap(eq("T1"), any(), any(), any())).thenThrow(new RuntimeException("db"));

        assertThat(service.match("en").matched()).isEqualTo(1);
        verify(mapper).upsertLangMap("T2", "en", "E2", "COORD_NAME");
    }

    @Test
    @DisplayName("전체 매칭 후 최종 표시값을 한 번에 다시 계산한다(원문이 바뀐 관광지의 오래된 번역 제거)")
    void matchAllResolves() {
        when(mapper.koreanToursForMatch()).thenReturn(List.of());
        when(mapper.langContentsForMatch(any())).thenReturn(List.of());

        service.matchAll();

        verify(mapper).resolveTourColumns(List.of());
    }
}
