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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("[번역 저장소] TourTranslationService")
class TourTranslationServiceTest {

    @Mock TourTranslationMapper mapper;
    TourTranslationService service;

    @BeforeEach
    void setUp() {
        service = new TourTranslationService(mapper);
    }

    @Test
    @DisplayName("정상 AI 번역은 원문과 함께 AI 방식으로 저장하고 해당 관광지 최종값만 다시 계산한다")
    void savesWithSource() {
        assertThat(service.saveAi("T1", "TOUR_NM", "en", "경복궁", " Gyeongbokgung Palace ")).isTrue();

        verify(mapper).upsertTranslation("T1", "TOUR_NM", "en", "AI", "경복궁", "Gyeongbokgung Palace");
        verify(mapper).resolveTourColumns(List.of("T1"));
    }

    @Test
    @DisplayName("실패한 번역(빈 값/원문 그대로/이름·주소에 한글 잔존)은 저장하지 않는다")
    void rejectsFailedTranslations() {
        assertThat(service.saveAi("T1", "TOUR_NM", "en", "경복궁", "")).isFalse();
        assertThat(service.saveAi("T1", "TOUR_NM", "en", "경복궁", "경복궁")).isFalse();
        assertThat(service.saveAi("T1", "ROAD_ADDR", "en", "서울 종로구", "Seoul 종로구")).isFalse();
        assertThat(service.saveAi("T1", "TOUR_NM", "en", "", "x")).isFalse(); // 원문 없음 → 해시 불가

        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("설명(TOUR_CONTENT)은 고유명사 병기로 한글이 섞여도 정상 번역으로 본다")
    void overviewMayContainHangul() {
        assertThat(TourTranslationService.isValid("TOUR_CONTENT", "경복궁은 법궁이다", "Gyeongbokgung (경복궁) is the main palace")).isTrue();
    }

    @Test
    @DisplayName("원문과 대상 언어가 같으면(ko) 저장하지 않는다")
    void sameLanguageSkipped() {
        assertThat(service.saveAi("T1", "TOUR_NM", "ko", "경복궁", "Gyeongbokgung")).isFalse();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("저장 실패는 AI 실패와 구분해 false만 돌려주고 예외를 던지지 않는다")
    void saveFailureIsSwallowed() {
        when(mapper.upsertTranslation(any(), any(), any(), any(), any(), any())).thenThrow(new RuntimeException("db down"));

        assertThat(service.saveAi("T1", "TOUR_NM", "en", "경복궁", "Gyeongbokgung")).isFalse();
    }

    @Test
    @DisplayName("묶음 저장은 정상 번역만 저장하고 최종값 재계산은 한 번만 한다")
    void saveAllResolvesOnce() {
        int saved = service.saveAiAll("en", List.of(
                new TourTranslationService.Entry("T1", "TOUR_NM", "경복궁", "Gyeongbokgung"),
                new TourTranslationService.Entry("T2", "TOUR_NM", "남산", "남산"),           // 실패
                new TourTranslationService.Entry("T3", "ROAD_ADDR", "서울 중구", "Jung-gu, Seoul")));

        assertThat(saved).isEqualTo(2);
        verify(mapper, never()).upsertTranslation(eq("T2"), any(), any(), any(), any(), any());
        verify(mapper, times(1)).resolveTourColumns(List.of("T1", "T3"));
    }

    @Test
    @DisplayName("수동 번역도 MANUAL 방식으로 같은 저장 경로를 탄다")
    void manualSave() {
        service.saveManual("T1", "TOUR_NM", "ja", "경복궁", "景福宮");
        verify(mapper).upsertTranslation("T1", "TOUR_NM", "ja", "MANUAL", "경복궁", "景福宮");
    }

    @Test
    @DisplayName("[동시성] 같은 (관광지, 필드, 언어)는 동시에 한 요청만 번역 권한을 얻는다")
    void concurrentClaim() throws Exception {
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                start.await();
                if (service.tryClaim("T1", "TOUR_NM", "en")) winners.incrementAndGet();
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(winners.get()).isEqualTo(1);
        assertThat(service.tryClaim("T1", "TOUR_NM", "ja")).as("언어가 다르면 별개").isTrue();
        service.release("T1", "TOUR_NM", "en");
        assertThat(service.tryClaim("T1", "TOUR_NM", "en")).as("해제 후 다시 획득 가능").isTrue();
    }
}
