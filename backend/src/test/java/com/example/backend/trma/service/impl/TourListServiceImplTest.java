package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.TourAiDetailData;
import com.example.backend.trma.dto.dataList.TourDetailData;
import com.example.backend.trma.dto.dataList.TourSearchData;
import com.example.backend.trma.dto.request.TourAiDetailRequest;
import com.example.backend.trma.dto.request.TourSearchRequest;
import com.example.backend.trma.dto.response.TourAiDetailResponse;
import com.example.backend.trma.mapper.MoimListMapper;
import com.example.backend.trma.mapper.TourListMapper;
import com.example.backend.trma.mapper.TourTranslationMapper;
import com.example.backend.trma.service.TourIntroService;
import com.example.backend.trma.service.TourTranslationService;
import com.example.backend.trma.util.AiErrorUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("[여행지] TourListService")
class TourListServiceImplTest {

    static final String GEMINI_URL = "http://gemini.test/generate";

    @Mock TourListMapper tourListMapper;
    @Mock MoimListMapper moimListMapper;
    @Mock TourTranslationMapper translationMapper;
    @Mock TourIntroService tourIntroService;
    TourTranslationService translationService;
    MockRestServiceServer gemini;
    TourListServicelmpl service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        gemini = MockRestServiceServer.bindTo(builder).build();
        translationService = new TourTranslationService(translationMapper);
        service = new TourListServicelmpl(tourListMapper, moimListMapper, translationService, tourIntroService, builder.build());
        ReflectionTestUtils.setField(service, "url", GEMINI_URL);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        AiErrorUtil.clearQuotaBlock();
    }

    @AfterEach
    void tearDown() {
        AiErrorUtil.clearQuotaBlock();
    }

    /** Gemini generateContent 응답 모양으로 text를 감싼다. */
    static String geminiReply(Object payload) {
        ObjectMapper om = new ObjectMapper();
        String text = payload instanceof String s ? s : om.writeValueAsString(payload);
        return om.writeValueAsString(Map.of("candidates", List.of(
                Map.of("content", Map.of("parts", List.of(Map.of("text", text)))))));
    }

    void geminiReturns(Object payload) {
        gemini.expect(ExpectedCount.once(), requestTo(GEMINI_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(geminiReply(payload), MediaType.APPLICATION_JSON));
    }

    static TourSearchData tour(String id, String nm, String cachedEn) {
        TourSearchData t = new TourSearchData();
        t.setTourId(id);
        t.setTourNm(nm);
        t.setTourNmEn(cachedEn);
        t.setNativeMatchYn("N");
        return t;
    }

    // ───────────────────────── 목록/이름 번역 fallback ─────────────────────────

    @Nested
    @DisplayName("관광지 이름 번역 (수동 번역 우선 → 없으면 Gemini → 실패 시 한국어)")
    class NameTranslation {

        @Test
        @DisplayName("수동 번역이 있으면 Gemini를 호출하지 않고 그 값을 쓴다")
        void manualTranslationWins() {
            when(tourListMapper.tourByIds(any(), eq("en"))).thenReturn(List.of(tour("T1", "경복궁", "Gyeongbokgung (manual)")));

            Map<String, String> result = service.translateTourNames(List.of("T1"), "en");

            assertThat(result).containsEntry("T1", "Gyeongbokgung (manual)");
            gemini.verify(); // 요청이 하나도 없어야 한다
            verify(translationMapper, never()).upsertTranslation(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("공식 영문 데이터(NATIVE_MATCH_YN=Y)가 있으면 Gemini를 호출하지 않는다")
        void nativeDataWins() {
            TourSearchData t = tour("T1", "Gyeongbokgung Palace", null);
            t.setNativeMatchYn("Y");
            when(tourListMapper.tourByIds(any(), eq("en"))).thenReturn(List.of(t));

            assertThat(service.translateTourNames(List.of("T1"), "en")).containsEntry("T1", "Gyeongbokgung Palace");
            gemini.verify();
        }

        @Test
        @DisplayName("수동 번역이 없으면 Gemini로 번역하고 영어 컬럼에만 캐시한다")
        void fallbackToGeminiAndCache() {
            when(tourListMapper.tourByIds(any(), eq("en"))).thenReturn(List.of(tour("T1", "경복궁", null)));
            geminiReturns(Map.of("translations", List.of(Map.of("tourId", "T1", "tourNm", "Gyeongbokgung Palace"))));

            Map<String, String> result = service.translateTourNames(List.of("T1"), "en");

            assertThat(result).containsEntry("T1", "Gyeongbokgung Palace");
            verify(translationMapper).upsertTranslation("T1", "TOUR_NM", "en", "AI", "경복궁", "Gyeongbokgung Palace");
            verify(translationMapper).resolveTourColumns(List.of("T1"));
            gemini.verify();
        }

        @Test
        @DisplayName("일본어 요청이면 일본어 컬럼에만 캐시한다")
        void japaneseColumn() {
            TourSearchData t = tour("T1", "경복궁", null);
            when(tourListMapper.tourByIds(any(), eq("ja"))).thenReturn(List.of(t));
            geminiReturns(Map.of("translations", List.of(Map.of("tourId", "T1", "tourNm", "景福宮"))));

            assertThat(service.translateTourNames(List.of("T1"), "ja")).containsEntry("T1", "景福宮");
            verify(translationMapper).upsertTranslation("T1", "TOUR_NM", "ja", "AI", "경복궁", "景福宮");
        }

        @Test
        @DisplayName("Gemini가 한국어 원문을 그대로 돌려주면 저장하지 않고 원문을 보여준다")
        void rejectsHangulResult() {
            when(tourListMapper.tourByIds(any(), eq("en"))).thenReturn(List.of(tour("T1", "경복궁", null)));
            geminiReturns(Map.of("translations", List.of(Map.of("tourId", "T1", "tourNm", "경복궁"))));

            assertThat(service.translateTourNames(List.of("T1"), "en")).containsEntry("T1", "경복궁");
            verify(translationMapper, never()).upsertTranslation(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Gemini 호출 실패(4xx)면 예외 없이 한국어 원문을 보여준다")
        void geminiErrorFallsBackToKorean() {
            when(tourListMapper.tourByIds(any(), eq("en"))).thenReturn(List.of(tour("T1", "경복궁", null)));
            gemini.expect(requestTo(GEMINI_URL)).andRespond(withBadRequest());

            assertThat(service.translateTourNames(List.of("T1"), "en")).containsEntry("T1", "경복궁");
            verify(translationMapper, never()).upsertTranslation(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Gemini 과부하(503)면 재시도 후 포기하고 한국어 원문을 보여준다")
        void geminiOverloadedRetriesThenFallsBack() {
            when(tourListMapper.tourByIds(any(), eq("en"))).thenReturn(List.of(tour("T1", "경복궁", null)));
            gemini.expect(ExpectedCount.times(4), requestTo(GEMINI_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

            assertThat(service.translateTourNames(List.of("T1"), "en")).containsEntry("T1", "경복궁");
            gemini.verify();
        }

        @Test
        @DisplayName("일일 한도 초과(429 PerDay)면 재시도하지 않고, 이후 요청은 Gemini를 부르지 않고 바로 한국어로 응답한다")
        void dailyQuotaNoRetryThenSkip() {
            when(tourListMapper.tourByIds(any(), eq("en"))).thenReturn(List.of(tour("T1", "경복궁", null)));
            gemini.expect(ExpectedCount.once(), requestTo(GEMINI_URL))
                    .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
                            .body("{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\",\"details\":[{\"quotaId\":\"GenerateRequestsPerDayPerProjectPerModel-FreeTier\"}]}}"));

            long start = System.currentTimeMillis();
            assertThat(service.translateTourNames(List.of("T1"), "en")).containsEntry("T1", "경복궁");
            // 두 번째 요청: 차단 중이라 Gemini 요청 자체가 없어야 한다(ExpectedCount.once 초과 시 실패).
            assertThat(service.translateTourNames(List.of("T1"), "en")).containsEntry("T1", "경복궁");
            long elapsed = System.currentTimeMillis() - start;

            gemini.verify();
            assertThat(elapsed).as("재시도 대기(0.5+1+1.5초)가 없어야 한다").isLessThan(1_000);
        }

        @Test
        @DisplayName("분당 한도라도 retryDelay가 길면(57초) 재시도하지 않고 그동안 호출을 쉰다")
        void longRetryDelayNoRetry() {
            when(tourListMapper.tourByIds(any(), eq("en"))).thenReturn(List.of(tour("T1", "경복궁", null)));
            gemini.expect(ExpectedCount.once(), requestTo(GEMINI_URL))
                    .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
                            .body("{\"error\":{\"details\":[{\"quotaId\":\"GenerateRequestsPerMinutePerProjectPerModel-FreeTier\"},{\"retryDelay\":\"57s\"}]}}"));

            assertThat(service.translateTourNames(List.of("T1"), "en")).containsEntry("T1", "경복궁");
            assertThat(service.translateTourNames(List.of("T1"), "en")).containsEntry("T1", "경복궁");
            gemini.verify();
        }

        @Test
        @DisplayName("retryDelay 정보가 없는 짧은 429는 일시적이므로 기존처럼 재시도한다")
        void perMinuteLimitRetries() {
            when(tourListMapper.tourByIds(any(), eq("en"))).thenReturn(List.of(tour("T1", "경복궁", null)));
            gemini.expect(ExpectedCount.times(4), requestTo(GEMINI_URL))
                    .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
                            .body("{\"error\":{\"details\":[{\"quotaId\":\"GenerateRequestsPerMinutePerProjectPerModel-FreeTier\"}]}}"));

            assertThat(service.translateTourNames(List.of("T1"), "en")).containsEntry("T1", "경복궁");
            gemini.verify();
        }

        @Test
        @DisplayName("Gemini 응답이 JSON이 아니면 한국어 원문을 보여준다")
        void brokenJsonFallsBack() {
            when(tourListMapper.tourByIds(any(), eq("en"))).thenReturn(List.of(tour("T1", "경복궁", null)));
            geminiReturns("죄송하지만 번역할 수 없습니다.");

            assertThat(service.translateTourNames(List.of("T1"), "en")).containsEntry("T1", "경복궁");
        }

        @Test
        @DisplayName("캐시 저장(DB)이 실패해도 이번 응답에는 번역값을 보여준다")
        void saveFailureStillShowsTranslation() {
            when(tourListMapper.tourByIds(any(), eq("en"))).thenReturn(List.of(tour("T1", "경복궁", null), tour("T2", "남산", null)));
            when(translationMapper.upsertTranslation(eq("T1"), any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("db down"));
            geminiReturns(Map.of("translations", List.of(
                    Map.of("tourId", "T1", "tourNm", "Gyeongbokgung Palace"),
                    Map.of("tourId", "T2", "tourNm", "Namsan"))));

            Map<String, String> result = service.translateTourNames(List.of("T1", "T2"), "en");

            assertThat(result).containsEntry("T1", "Gyeongbokgung Palace").containsEntry("T2", "Namsan");
        }

        @Test
        @DisplayName("한국어 요청이면 번역을 시도하지 않는다")
        void koreanSkips() {
            assertThat(service.translateTourNames(List.of("T1"), "ko")).isEmpty();
            verifyNoInteractions(tourListMapper);
            gemini.verify();
        }
    }

    // ───────────────────────── 상세 번역 fallback ─────────────────────────

    @Nested
    @DisplayName("관광지 상세 번역")
    class DetailTranslation {

        TourDetailData detail() {
            TourDetailData d = new TourDetailData();
            d.setTourId("T1");
            d.setTourNm("경복궁");
            d.setOverview("조선 왕조의 법궁");
            d.setRoadAddr("서울특별시 종로구 사직로 161");
            d.setNativeMatchYn("N");
            return d;
        }

        void apply(TourDetailData d, String lang) {
            ReflectionTestUtils.invokeMethod(service, "applyDetailTranslation", d, lang);
        }

        @Test
        @DisplayName("모든 필드에 수동 번역이 있으면 Gemini 없이 그대로 쓴다")
        void allCached() {
            TourDetailData d = detail();
            d.setTourNmEn("Gyeongbokgung");
            d.setOverviewEn("Main palace");
            d.setRoadAddrEn("161 Sajik-ro, Jongno-gu, Seoul");

            apply(d, "en");

            assertThat(d.getTourNm()).isEqualTo("Gyeongbokgung");
            assertThat(d.getOverview()).isEqualTo("Main palace");
            assertThat(d.getRoadAddr()).isEqualTo("161 Sajik-ro, Jongno-gu, Seoul");
            gemini.verify();
        }

        @Test
        @DisplayName("일부만 수동 번역돼 있으면 빈 필드만 Gemini로 채우고, 수동 번역은 덮어쓰지 않는다")
        void fillsOnlyMissingFields() {
            TourDetailData d = detail();
            d.setTourNmEn("Gyeongbokgung (manual)");
            // 프롬프트에는 수동 번역이 아니라 한국어 원문이 들어가야 한다.
            gemini.expect(requestTo(GEMINI_URL))
                    .andExpect(content().string(allOf(containsString("경복궁"), containsString("사직로"), not(containsString("(manual)")))))
                    .andRespond(withSuccess(geminiReply(Map.of("translations", List.of(Map.of(
                            "tourId", "T1", "tourNm", "Gyeongbokgung Palace (AI)", "overview", "Main royal palace",
                            "roadAddr", "161 Sajik-ro, Jongno-gu, Seoul", "detailAddr", "")))), MediaType.APPLICATION_JSON));

            apply(d, "en");

            assertThat(d.getTourNm()).isEqualTo("Gyeongbokgung (manual)");
            assertThat(d.getOverview()).isEqualTo("Main royal palace");
            assertThat(d.getRoadAddr()).isEqualTo("161 Sajik-ro, Jongno-gu, Seoul");
            // 이름(tourNmEn)은 null로 넘겨 저장 대상에서 제외한다.
            verify(translationMapper).upsertTranslation("T1", "TOUR_CONTENT", "en", "AI", "조선 왕조의 법궁", "Main royal palace");
            verify(translationMapper).upsertTranslation("T1", "ROAD_ADDR", "en", "AI", "서울특별시 종로구 사직로 161", "161 Sajik-ro, Jongno-gu, Seoul");
            verify(translationMapper, never()).upsertTranslation(any(), eq("TOUR_NM"), any(), any(), any(), any());
            gemini.verify();
        }

        @Test
        @DisplayName("이름/주소는 캐시돼 있고 설명만 없으면 설명만 작은 프롬프트로 번역한다")
        void overviewOnly() {
            TourDetailData d = detail();
            d.setTourNmEn("Gyeongbokgung");
            d.setRoadAddrEn("161 Sajik-ro");
            geminiReturns(Map.of("reviewTranslations", List.of(Map.of("reviewId", "T1", "reviewContent", "Main palace"))));

            apply(d, "en");

            assertThat(d.getOverview()).isEqualTo("Main palace");
            verify(translationMapper).upsertTranslation("T1", "TOUR_CONTENT", "en", "AI", "조선 왕조의 법궁", "Main palace");
        }

        @Test
        @DisplayName("상세주소만 비어 있어도 번역 대상으로 본다")
        void detailAddrMissingTriggersTranslation() {
            TourDetailData d = detail();
            d.setTourNmEn("Gyeongbokgung");
            d.setOverviewEn("Main palace");
            d.setRoadAddrEn("161 Sajik-ro");
            d.setDetailAddr("경복궁 내 흥례문");
            geminiReturns(Map.of("translations", List.of(Map.of(
                    "tourId", "T1", "tourNm", "x", "overview", "x", "roadAddr", "x", "detailAddr", "Heungnyemun Gate"))));

            apply(d, "en");

            assertThat(d.getDetailAddr()).isEqualTo("Heungnyemun Gate");
            assertThat(d.getTourNm()).isEqualTo("Gyeongbokgung");
            verify(translationMapper).upsertTranslation("T1", "DETAIL_ADDR", "en", "AI", "경복궁 내 흥례문", "Heungnyemun Gate");
            verify(translationMapper, never()).upsertTranslation(any(), eq("TOUR_NM"), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Gemini가 주소를 한글로 돌려주면 그 주소는 저장/표시하지 않는다")
        void hangulAddressRejected() {
            TourDetailData d = detail();
            geminiReturns(Map.of("translations", List.of(Map.of(
                    "tourId", "T1", "tourNm", "Gyeongbokgung", "overview", "Main palace", "roadAddr", "서울특별시 종로구", "detailAddr", ""))));

            apply(d, "en");

            assertThat(d.getRoadAddr()).isEqualTo("서울특별시 종로구 사직로 161");
            verify(translationMapper).upsertTranslation("T1", "TOUR_NM", "en", "AI", "경복궁", "Gyeongbokgung");
            verify(translationMapper).upsertTranslation("T1", "TOUR_CONTENT", "en", "AI", "조선 왕조의 법궁", "Main palace");
            verify(translationMapper, never()).upsertTranslation(any(), eq("ROAD_ADDR"), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Gemini 실패 시 캐시값은 유지하고 나머지는 한국어로 둔다")
        void geminiFailure() {
            TourDetailData d = detail();
            d.setTourNmEn("Gyeongbokgung");
            gemini.expect(requestTo(GEMINI_URL)).andRespond(withServerError());

            apply(d, "en");

            assertThat(d.getTourNm()).isEqualTo("Gyeongbokgung");
            assertThat(d.getRoadAddr()).isEqualTo("서울특별시 종로구 사직로 161");
            verify(translationMapper, never()).upsertTranslation(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("원본 주소가 없는 코스형 관광지는 주소 번역 대기로 보지 않는다")
        void courseWithoutAddress() {
            TourDetailData d = detail();
            d.setRoadAddr("");
            d.setTourNmEn("Walking Course");
            d.setOverviewEn("A course");

            apply(d, "en");

            assertThat(d.getTourNm()).isEqualTo("Walking Course");
            gemini.verify();
        }
    }

    // ───────────────────────── 이용정보 (TourAPI 기반) ─────────────────────────

    @Nested
    @DisplayName("관광지 이용정보 (TourAPI 기반, Gemini로 지어내지 않음)")
    class UsageInfo {

        TourAiDetailRequest request(String lang) {
            TourAiDetailRequest r = new TourAiDetailRequest();
            r.setTourId("1017568");
            r.setLang(lang);
            return r;
        }

        @Test
        @DisplayName("TourIntroService 결과를 그대로 돌려주고 Gemini에게 이용정보를 묻지 않는다")
        void delegatesWithoutAskingGemini() {
            TourAiDetailData data = new TourAiDetailData();
            data.setOperatingHours("09:00~18:00");
            data.setSource("TOURAPI");
            when(tourIntroService.usageInfo(eq("1017568"), eq("ko"), any())).thenReturn(data);

            TourAiDetailResponse response = service.tourAiDetail(request("ko"));

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getOperatingHours()).isEqualTo("09:00~18:00");
            gemini.verify();
            verify(tourListMapper, never()).upsertTourAiInfo(any(), any(), any());
        }

        @Test
        @DisplayName("언어가 비어 있으면 ko로 조회한다")
        void blankLangIsKorean() {
            when(tourIntroService.usageInfo(any(), any(), any())).thenReturn(new TourAiDetailData());
            service.tourAiDetail(request(""));
            verify(tourIntroService).usageInfo(eq("1017568"), eq("ko"), any());
        }

        @Test
        @DisplayName("이용정보 조회가 예외로 실패해도 500 응답으로 감싸고 죽지 않는다")
        void failureWrapped() {
            when(tourIntroService.usageInfo(any(), any(), any())).thenThrow(new RuntimeException("boom"));
            assertThat(service.tourAiDetail(request("en")).getStatus()).isEqualTo(500);
        }
    }

    // ───────────────────────── 동시 요청 중복 방지 ─────────────────────────

    @Test
    @DisplayName("[중복 방지] 같은 관광지 이름을 다른 요청이 번역 중이면 Gemini를 다시 부르지 않는다")
    void inFlightDedupe() {
        when(tourListMapper.tourByIds(any(), eq("en"))).thenReturn(List.of(tour("T1", "경복궁", null)));
        assertThat(translationService.tryClaim("T1", "TOUR_NM", "en")).isTrue(); // 다른 요청이 선점

        assertThat(service.translateTourNames(List.of("T1"), "en")).containsEntry("T1", "경복궁");
        gemini.verify(); // 요청 없음

        translationService.release("T1", "TOUR_NM", "en");
        geminiReturns(Map.of("translations", List.of(Map.of("tourId", "T1", "tourNm", "Gyeongbokgung Palace"))));
        assertThat(service.translateTourNames(List.of("T1"), "en")).containsEntry("T1", "Gyeongbokgung Palace");
        // 번역이 끝나면 잠금이 풀린다
        assertThat(translationService.tryClaim("T1", "TOUR_NM", "en")).isTrue();
    }

    // ───────────────────────── 검색 ─────────────────────────

    @Test
    @DisplayName("[검색] page=0은 1페이지로 보고, 페이지당 10건 offset을 계산한다")
    void searchPaging() {
        when(tourListMapper.tourSearch(any())).thenReturn(List.of());
        TourSearchRequest request = new TourSearchRequest();
        request.setLang("ko");

        request.setPage(0);
        service.tourSearch(request);
        assertThat(request.getPage()).isEqualTo(1);
        assertThat(request.getOffset()).isZero();

        request.setPage(3);
        service.tourSearch(request);
        assertThat(request.getOffset()).isEqualTo(20);
    }

    @Test
    @DisplayName("[검색] 수동 번역은 바로 적용하고, 빈 것은 Gemini를 기다리지 않고 응답한 뒤 백그라운드에서 캐시에 채운다")
    void searchDoesNotWaitForGemini() {
        TourSearchData manual = tour("T1", "경복궁", "Gyeongbokgung (manual)");
        manual.setRoadAddr("서울 종로구");
        manual.setRoadAddrEn("Jongno-gu, Seoul");
        TourSearchData empty = tour("T2", "남산", null);
        empty.setRoadAddr("서울 중구");
        when(tourListMapper.tourSearch(any())).thenReturn(List.of(manual, empty));
        gemini.expect(ExpectedCount.manyTimes(), requestTo(GEMINI_URL))
                .andRespond(withSuccess(geminiReply(Map.of(
                        "translations", List.of(Map.of("tourId", "T2", "tourNm", "Namsan")),
                        "reviewTranslations", List.of(Map.of("reviewId", "T2", "reviewContent", "Jung-gu, Seoul")))), MediaType.APPLICATION_JSON));
        TourSearchRequest request = new TourSearchRequest();
        request.setLang("en");

        List<TourSearchData> data = service.tourSearch(request).getData();

        assertThat(data.get(0).getTourNm()).isEqualTo("Gyeongbokgung (manual)");
        assertThat(data.get(0).getRoadAddr()).isEqualTo("Jongno-gu, Seoul");
        // 응답 시점에는 아직 한국어(기다리지 않음), 응답 객체는 백그라운드에서 바뀌지 않는다.
        assertThat(data.get(1).getTourNm()).isEqualTo("남산");
        verify(translationMapper, timeout(3_000)).upsertTranslation("T2", "TOUR_NM", "en", "AI", "남산", "Namsan");
        verify(translationMapper, timeout(3_000)).upsertTranslation("T2", "ROAD_ADDR", "en", "AI", "서울 중구", "Jung-gu, Seoul");
        assertThat(data.get(1).getTourNm()).isEqualTo("남산");
    }

    @Test
    @DisplayName("[검색] DB 오류 시 500 실패 응답")
    void searchDbError() {
        when(tourListMapper.tourSearch(any())).thenThrow(new RuntimeException("db down"));
        TourSearchRequest request = new TourSearchRequest();
        request.setLang("ko");

        assertThat(service.tourSearch(request).getStatus()).isEqualTo(500);
    }

    // ───────────────────────── 사용자 콘텐츠 자유 텍스트 번역(ko/en/ja) ─────────────────────────

    @Nested
    @DisplayName("사용자 콘텐츠 번역(translateFreeTexts)")
    class FreeTexts {
        static Map<String, Object> reply(String key, String text) {
            return Map.of("reviewTranslations", List.of(Map.of("reviewId", key, "reviewContent", text)));
        }

        @Test
        @DisplayName("외국어로 쓴 글을 한국어로 번역하고, 이미 한국어인 글은 AI 없이 그대로 돌려준다")
        void koreanTarget() {
            geminiReturns(reply("a", "전망이 정말 좋아요"));

            Map<String, String> result = service.translateFreeTexts(Map.of("a", "The view is great", "b", "좋았어요"), "ko");

            assertThat(result).containsEntry("a", "전망이 정말 좋아요").containsEntry("b", "좋았어요");
            gemini.verify();
        }

        @Test
        @DisplayName("모두 대상 언어로 쓰였으면 Gemini를 부르지 않는다")
        void sameLanguageSkipsAi() {
            assertThat(service.translateFreeTexts(Map.of("a", "Hello there"), "en")).containsEntry("a", "Hello there");
            gemini.verify();
        }

        @Test
        @DisplayName("원문을 그대로 돌려주거나 다른 언어로 답한 번역은 버린다")
        void rejectsInvalidResult() {
            geminiReturns(reply("a", "The view is great"));
            assertThat(service.translateFreeTexts(Map.of("a", "The view is great"), "ko")).isEmpty();
        }

        @Test
        @DisplayName("Gemini가 실패해도 예외 없이 같은 언어 항목만 돌려준다")
        void geminiFailure() {
            gemini.expect(ExpectedCount.manyTimes(), requestTo(GEMINI_URL))
                    .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());
            assertThat(service.translateFreeTexts(Map.of("a", "Nice view", "b", "좋아요"), "ko"))
                    .containsExactly(Map.entry("b", "좋아요"));
        }

        @Test
        @DisplayName("지원하지 않는 언어는 번역하지 않는다")
        void unsupported() {
            assertThat(service.translateFreeTexts(Map.of("a", "Nice view"), "zh")).isEmpty();
            gemini.verify();
        }
    }
}
