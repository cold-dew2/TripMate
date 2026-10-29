package com.example.backend.trma.service;

import com.example.backend.trma.dto.dataList.TourAiDetailData;
import com.example.backend.trma.dto.dataList.TourInfoAiData;
import com.example.backend.trma.dto.dataList.TourIntroData;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

// 매퍼는 메모리 저장소처럼 동작하게 만들어(저장한 값을 다음 조회에서 그대로 돌려줌),
// "실제로 저장됐는지"와 "두 번째 조회에서 AI를 다시 부르지 않는지"를 함께 검증한다.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("[이용정보] TourIntroService - A.정보 보완(TourAPI → 저장 조사값 → AI 조사) / B.번역 보완")
class TourIntroServiceTest {

    static final String GEMINI = "http://gemini.test/generate";

    @Mock TourTranslationMapper mapper;
    TourTranslationService translationService;
    TourInfoResearchService researchService;
    MockRestServiceServer server;
    TourIntroService service;

    // ── 메모리 저장소 ──
    final Map<String, TourInfoAiData> infoAi = new LinkedHashMap<>();          // FIELD_NM → 조사 결과
    final Map<String, String[]> trans = new LinkedHashMap<>();                 // field|lang|type → [src, text]
    final AtomicInteger aiTranslateCalls = new AtomicInteger();

    static final String KO_INTRO = """
            {"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[{"contentid":"126508","contenttypeid":"12",
             "infocenter":"02-3700-3900","restdate":"매주 화요일","usetime":"09:00~18:00<br>(입장마감 17:00)",
             "parking":"가능 (승용차 240대)","expguide":""}]}}}}""";
    static final String KO_COMMON = """
            {"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[{"contentid":"126508","contenttypeid":"12",
             "tel":"","homepage":"<a href=\\"https://royal.khs.go.kr/\\" target=\\"_blank\\">royal.khs.go.kr</a>",
             "overview":"경복궁은 조선의 법궁이다."}]}}}}""";
    static final String EN_INTRO = """
            {"response":{"body":{"items":{"item":[{"contentid":"264337","contenttypeid":"76",
             "infocenter":"+82-2-3700-3900","restdate":"Tuesdays","usetime":"09:00-18:00","parking":""}]}}}}""";
    static final String EN_COMMON = """
            {"response":{"body":{"items":{"item":[{"contentid":"264337","homepage":"","tel":"",
             "overview":"Gyeongbokgung Palace was built in 1395."}]}}}}""";

    @BeforeEach
    void setUp() {
        AiErrorUtil.clearQuotaBlock();
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        RestClient rc = builder.build();
        translationService = new TourTranslationService(mapper);
        researchService = new TourInfoResearchService(mapper, translationService, rc);
        ReflectionTestUtils.setField(researchService, "url", GEMINI);
        ReflectionTestUtils.setField(researchService, "apiKey", "k");
        researchService.executor = Runnable::run; // 조사도 요청 안에서 동기로 끝내 결과를 바로 검증
        service = new TourIntroService(mapper, translationService, rc, researchService);
        service.translateExecutor = Runnable::run;
        ReflectionTestUtils.setField(service, "host", "tour.test");
        ReflectionTestUtils.setField(service, "apiKey", "k");
        ReflectionTestUtils.setField(service, "korCommonPath", "B551011/KorService2/detailCommon2");
        ReflectionTestUtils.setField(service, "engCommonPath", "B551011/EngService2/detailCommon2");
        ReflectionTestUtils.setField(service, "jpnCommonPath", "B551011/JpnService2/detailCommon2");

        when(mapper.contentTypeId("126508", "ko")).thenReturn("12");
        when(mapper.contentTypeId("264337", "en")).thenReturn("76");
        when(mapper.tourContent("126508")).thenReturn("경복궁은 조선의 법궁이다.");
        when(mapper.tourBasic("126508")).thenReturn(Map.of("tourId", "126508", "tourNm", "경복궁", "roadAddr", "서울특별시 종로구 사직로 161"));

        // 조사 결과 저장/조회
        when(mapper.upsertInfoAi(any())).thenAnswer(inv -> {
            TourInfoAiData d = inv.getArgument(0);
            d.setCheckedDt(LocalDateTime.now());
            infoAi.put(d.getFieldNm(), d);
            return 1;
        });
        when(mapper.selectInfoAi("126508")).thenAnswer(inv -> new ArrayList<>(infoAi.values()));
        // 번역 저장/조회 (실제 SQL과 같은 우선순위 MANUAL > TOURAPI > LEGACY > AI, 방식별 별도 행)
        when(mapper.upsertTranslation(eq("126508"), anyString(), anyString(), anyString(), anyString(), anyString())).thenAnswer(inv -> {
            trans.put(inv.getArgument(1) + "|" + inv.getArgument(2) + "|" + inv.getArgument(3),
                    new String[]{((String) inv.getArgument(4)).trim(), inv.getArgument(5)});
            return 1;
        });
        when(mapper.bestTranslations(eq("126508"), anyString(), eq("INTRO_"))).thenAnswer(inv -> {
            String lang = inv.getArgument(1);
            Map<String, Map<String, String>> best = new LinkedHashMap<>();
            for (String type : List.of("AI", "LEGACY", "TOURAPI", "MANUAL")) { // 뒤가 우선 → 덮어씀
                trans.forEach((k, v) -> {
                    String[] p = k.split("\\|");
                    if (p[0].startsWith("INTRO_") && p[1].equals(lang) && p[2].equals(type)) {
                        best.put(p[0], Map.of("fieldNm", p[0], "transText", v[1], "transType", type,
                                "srcHash", TourIntroService.sha256(v[0])));
                    }
                });
            }
            return new ArrayList<>(best.values());
        });
    }

    @AfterEach
    void tearDown() {
        AiErrorUtil.clearQuotaBlock();
    }

    BiFunction<Map<String, String>, String, Map<String, String>> translator(Map<String, String> answer) {
        return (texts, lang) -> {
            aiTranslateCalls.incrementAndGet();
            Map<String, String> out = new LinkedHashMap<>();
            texts.keySet().forEach(k -> { if (answer.containsKey(k)) out.put(k, answer.get(k)); });
            return out;
        };
    }

    void expectKoTourApi() {
        server.expect(ExpectedCount.once(), requestTo(allOf(containsString("KorService2/detailCommon2"), containsString("contentId=126508"))))
                .andRespond(withSuccess(KO_COMMON, MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo(allOf(containsString("KorService2/detailIntro2"), containsString("contentTypeId=12"))))
                .andRespond(withSuccess(KO_INTRO, MediaType.APPLICATION_JSON));
    }

    /** google_search 근거가 붙은 Gemini 응답 */
    static String grounded(Map<String, Object> fields, List<Map<String, String>> chunks) {
        ObjectMapper om = new ObjectMapper();
        String text = om.writeValueAsString(Map.of("fields", fields));
        return om.writeValueAsString(Map.of("candidates", List.of(Map.of(
                "content", Map.of("parts", List.of(Map.of("text", text))),
                "groundingMetadata", Map.of("groundingChunks", chunks.stream().map(c -> Map.of("web", c)).toList())))));
    }

    static Map<String, Object> answer(String value, String url) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("value", value);
        m.put("sourceUrl", url);
        return m;
    }

    static final List<Map<String, String>> ROYAL = List.of(
            Map.of("uri", "https://vertexaisearch.cloud.google.com/grounding-api-redirect/abc", "title", "royal.khs.go.kr"));

    void expectResearch(String body) {
        server.expect(ExpectedCount.once(), requestTo(GEMINI))
                .andExpect(content().string(allOf(containsString("google_search"), containsString("경복궁"))))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    // 정보가 비어 있는 항목(요금/주차요금/문의/홈페이지)은 조사 결과 없음으로 미리 채워 조사 호출을 막는다.
    void markNotFound(String... fields) {
        for (String f : fields) {
            TourInfoAiData d = new TourInfoAiData();
            d.setTourId("126508");
            d.setFieldNm(f);
            d.setStatusCd("NOT_FOUND");
            d.setCheckedDt(LocalDateTime.now());
            infoAi.put(f, d);
        }
    }

    static TourIntroData cachedKo() {
        TourIntroData d = new TourIntroData();
        d.setTourId("126508");
        d.setLangCd("ko");
        d.setUseTime("09:00~18:00");
        d.setRestDate("매주 화요일");
        d.setParking("가능 (승용차 240대)");
        d.setHomepage("https://royal.khs.go.kr/");
        d.setFetchStatus("OK");
        d.setFetchDt(LocalDateTime.now().minusDays(1));
        return d;
    }

    // ───────────── 1. TourAPI 우선 ─────────────

    @Test
    @DisplayName("[1] TourAPI 국문 값은 그대로 쓰고(출처 TOURAPI), TourAPI에 있는 항목은 AI 조사 대상에서 뺀다")
    void tourApiFirst() {
        expectKoTourApi();
        server.expect(ExpectedCount.once(), requestTo(GEMINI))
                .andExpect(content().string(allOf(containsString("useFee"), not(containsString("- useTime")), not(containsString("- restDate")))))
                .andRespond(withSuccess(grounded(Map.of(), ROYAL), MediaType.APPLICATION_JSON));

        TourAiDetailData r = service.usageInfo("126508", "ko", translator(Map.of()));

        assertThat(r.getOperatingHours()).isEqualTo("09:00~18:00\n(입장마감 17:00)");
        assertThat(r.getClosedDays()).isEqualTo("매주 화요일");
        assertThat(r.getWebsiteUrl()).isEqualTo("https://royal.khs.go.kr/");
        assertThat(r.getFields().get("operatingHours").getOrigin()).isEqualTo("TOURAPI");
        assertThat(r.getAdmissionFeeIsFree()).isNull();
        assertThat(r.getParkingAvailable()).isNull();
        assertThat(aiTranslateCalls.get()).isZero();
        server.verify();
    }

    @Test
    @DisplayName("[1] TourAPI 값이 생기면 예전에 저장된 AI 조사값으로 대체하지 않는다")
    void tourApiBeatsStoredResearch() {
        when(mapper.selectIntro("126508", "ko")).thenReturn(cachedKo());
        TourInfoAiData old = new TourInfoAiData();
        old.setFieldNm("INTRO_REST_DATE");
        old.setStatusCd("FOUND");
        old.setInfoText("연중무휴");
        old.setSourceUrl("https://royal.khs.go.kr/x");
        old.setCheckedDt(LocalDateTime.now());
        infoAi.put("INTRO_REST_DATE", old);
        markNotFound("INTRO_USE_FEE", "INTRO_PARKING_FEE", "INTRO_INFO_CENTER");

        TourAiDetailData r = service.usageInfo("126508", "ko", null);

        assertThat(r.getClosedDays()).isEqualTo("매주 화요일");
        assertThat(r.getFields().get("closedDays").getOrigin()).isEqualTo("TOURAPI");
        server.verify(); // 조사 호출 없음
    }

    // ───────────── 2, 3, 5, 8. 정보 보완 ─────────────

    @Test
    @DisplayName("[3][5] 둘 다 없으면 AI가 조사해 출처와 함께 저장하고, 두 번째 조회는 AI 조사 없이 저장값을 쓴다")
    void researchThenReuse() {
        when(mapper.selectIntro("126508", "ko")).thenReturn(cachedKo());
        expectResearch(grounded(Map.of(
                "useFee", answer("대인(만25세~64세) 3,000원", "https://royal.khs.go.kr/ROYAL/contents/menuInfo-gbg.do"),
                "infoCenter", answer("02-3700-3900", "https://www.royal.khs.go.kr/gbg"),
                "parkingFee", answer(null, null)), ROYAL));

        TourAiDetailData first = service.usageInfo("126508", "ko", null);

        assertThat(first.isResearchPending()).isTrue();
        // 저장 확인: FOUND는 값+출처, 확인 못한 항목은 NOT_FOUND
        assertThat(infoAi.get("INTRO_USE_FEE").getStatusCd()).isEqualTo("FOUND");
        assertThat(infoAi.get("INTRO_USE_FEE").getSourceUrl()).isEqualTo("https://royal.khs.go.kr/ROYAL/contents/menuInfo-gbg.do");
        assertThat(infoAi.get("INTRO_USE_FEE").getSourceTitle()).isEqualTo("royal.khs.go.kr");
        assertThat(infoAi.get("INTRO_PARKING_FEE").getStatusCd()).isEqualTo("NOT_FOUND");
        assertThat(infoAi.get("INTRO_PARKING_FEE").getInfoText()).isNull();
        server.verify();

        // 두 번째 조회: Gemini 요청이 하나도 없어야 한다(추가 expect 없음 → 요청 시 실패)
        server.reset();
        TourAiDetailData second = service.usageInfo("126508", "ko", null);

        assertThat(second.isResearchPending()).isFalse();
        assertThat(second.getAdmissionFeeDetails()).isEqualTo("대인(만25세~64세) 3,000원");
        TourAiDetailData.FieldInfo fee = second.getFields().get("admissionFeeDetails");
        assertThat(fee.getOrigin()).isEqualTo("AI_RESEARCH");
        assertThat(fee.getSourceUrl()).startsWith("https://royal.khs.go.kr");
        assertThat(fee.getCheckedAt()).isNotNull();
        assertThat(second.getParkingFeeInfo()).as("출처를 못 찾은 항목은 노출하지 않음").isNull();
        server.verify();
    }

    @Test
    @DisplayName("[2] 저장된 AI 조사값(FOUND)이 있으면 다시 조사하지 않고 재사용한다")
    void storedResearchReused() {
        when(mapper.selectIntro("126508", "ko")).thenReturn(cachedKo());
        TourInfoAiData fee = new TourInfoAiData();
        fee.setFieldNm("INTRO_USE_FEE");
        fee.setStatusCd("FOUND");
        fee.setInfoText("성인 3,000원");
        fee.setSourceUrl("https://royal.khs.go.kr/fee");
        fee.setCheckedDt(LocalDateTime.now().minusDays(5));
        infoAi.put("INTRO_USE_FEE", fee);
        markNotFound("INTRO_PARKING_FEE", "INTRO_INFO_CENTER");

        TourAiDetailData r = service.usageInfo("126508", "ko", null);

        assertThat(r.getAdmissionFeeDetails()).isEqualTo("성인 3,000원");
        assertThat(r.isResearchPending()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("[8] 출처가 없거나, 검색 근거에 없는 도메인이거나, '확인 필요' 같은 값은 저장/노출하지 않는다")
    void unverifiedNotShown() {
        when(mapper.selectIntro("126508", "ko")).thenReturn(cachedKo());
        expectResearch(grounded(Map.of(
                "useFee", answer("성인 3,000원", null),                                  // 출처 없음
                "infoCenter", answer("02-1234-5678", "https://some-blog.example.com/p"),  // 근거에 없는 도메인
                "parkingFee", answer("확인 필요", "https://royal.khs.go.kr/p")), ROYAL));  // 모른다는 뜻

        service.usageInfo("126508", "ko", null);
        TourAiDetailData r = service.usageInfo("126508", "ko", null);

        assertThat(infoAi.values()).allMatch(d -> "NOT_FOUND".equals(d.getStatusCd()));
        assertThat(r.getAdmissionFeeDetails()).isNull();
        assertThat(r.getInfoCenter()).isNull();
        assertThat(r.getParkingFeeInfo()).isNull();
    }

    @Test
    @DisplayName("[8] 검색 근거(groundingChunks)가 없는 답은 모두 미확인으로 본다")
    void noGroundingNotTrusted() {
        when(mapper.selectIntro("126508", "ko")).thenReturn(cachedKo());
        expectResearch(grounded(Map.of("useFee", answer("성인 3,000원", "https://royal.khs.go.kr/fee")), List.of()));

        service.usageInfo("126508", "ko", null);

        assertThat(infoAi.get("INTRO_USE_FEE").getStatusCd()).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("AI 조사 호출 실패(402 크레딧 소진)는 '조사 실패'로 아무것도 저장하지 않고, 잠시 재조사하지 않는다")
    void researchFailureNotStored() {
        when(mapper.selectIntro("126508", "ko")).thenReturn(cachedKo());
        server.expect(ExpectedCount.once(), requestTo(GEMINI)).andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED));

        TourAiDetailData first = service.usageInfo("126508", "ko", null);
        TourAiDetailData second = service.usageInfo("126508", "ko", null);

        assertThat(infoAi).isEmpty();                     // NOT_FOUND와 구분: 저장 없음
        assertThat(first.getClosedDays()).isEqualTo("매주 화요일"); // TourAPI 값은 정상 표시
        assertThat(second.isResearchPending()).isFalse();
        server.verify();                                  // 두 번째는 호출 안 함(ExpectedCount.once)
    }

    @Test
    @DisplayName("다른 요청이 같은 관광지를 조사 중이면 중복 조사하지 않는다")
    void researchDedupe() {
        when(mapper.selectIntro("126508", "ko")).thenReturn(cachedKo());
        assertThat(translationService.tryClaim("126508", "RESEARCH", "ko")).isTrue();

        TourAiDetailData r = service.usageInfo("126508", "ko", null);

        assertThat(r.isResearchPending()).isTrue(); // 진행 중이니 잠시 뒤 다시 조회
        server.verify();                            // 요청 없음
    }

    @Test
    @DisplayName("TourAPI 조회 자체가 실패하면(무엇이 빠졌는지 모름) AI 조사도 하지 않고 빈 정보(NONE)")
    void tourApiFailureNoResearch() {
        server.expect(ExpectedCount.manyTimes(), requestTo(containsString("KorService2"))).andRespond(withServerError());

        TourAiDetailData r = service.usageInfo("126508", "ko", null);

        assertThat(r.getSource()).isEqualTo("NONE");
        assertThat(r.isResearchPending()).isFalse();
        verify(mapper).upsertIntro(argThat(d -> "FAIL".equals(d.getFetchStatus())));
    }

    // ───────────── 4, 5, 6, 7, 9. 번역 보완 ─────────────

    @Test
    @DisplayName("[4][5] 조사한 국문 값의 번역이 없으면 AI 번역 후 저장하고, 두 번째 조회는 조사도 번역도 다시 하지 않는다")
    void researchedValueTranslatedOnce() {
        when(mapper.selectIntro("126508", "ko")).thenReturn(cachedKo());
        TourInfoAiData fee = new TourInfoAiData();
        fee.setFieldNm("INTRO_USE_FEE");
        fee.setStatusCd("FOUND");
        fee.setInfoText("성인 3,000원");
        fee.setSourceUrl("https://royal.khs.go.kr/fee");
        fee.setCheckedDt(LocalDateTime.now());
        infoAi.put("INTRO_USE_FEE", fee);
        markNotFound("INTRO_PARKING_FEE", "INTRO_INFO_CENTER");
        var tr = translator(Map.of("INTRO_USE_FEE", "大人 3,000ウォン", "INTRO_REST_DATE", "毎週火曜日", "INTRO_PARKING", "可能（乗用車240台）"));

        TourAiDetailData first = service.usageInfo("126508", "ja", tr);

        assertThat(first.getAdmissionFeeDetails()).isEqualTo("大人 3,000ウォン");
        assertThat(first.getFields().get("admissionFeeDetails").getOrigin()).isEqualTo("AI_RESEARCH");
        assertThat(first.getFields().get("admissionFeeDetails").getTranslation()).isEqualTo("AI");
        assertThat(trans).containsKey("INTRO_USE_FEE|ja|AI");
        assertThat(trans.get("INTRO_USE_FEE|ja|AI")[0]).as("원문(조사값)도 함께 저장").isEqualTo("성인 3,000원");
        assertThat(aiTranslateCalls.get()).isEqualTo(1);

        TourAiDetailData second = service.usageInfo("126508", "ja", tr);

        assertThat(aiTranslateCalls.get()).as("두 번째 조회에서 AI 번역 없음").isEqualTo(1);
        assertThat(second.getAdmissionFeeDetails()).isEqualTo("大人 3,000ウォン");
        assertThat(second.getClosedDays()).isEqualTo("毎週火曜日");
        server.verify(); // 조사 호출 없음
    }

    @Test
    @DisplayName("[6] 국문 TourAPI 값이 있고 번역만 없으면 AI로 '조사'하지 않고 원문을 번역해 저장한다")
    void koreanExistsOnlyTranslate() {
        when(mapper.selectIntro("126508", "ko")).thenReturn(cachedKo());
        markNotFound("INTRO_USE_FEE", "INTRO_PARKING_FEE", "INTRO_INFO_CENTER");

        TourAiDetailData r = service.usageInfo("126508", "en",
                translator(Map.of("INTRO_REST_DATE", "Closed on Tuesdays", "INTRO_PARKING", "Available (240 cars)")));

        assertThat(r.getClosedDays()).isEqualTo("Closed on Tuesdays");
        assertThat(r.getFields().get("closedDays").getOrigin()).isEqualTo("TOURAPI");
        assertThat(r.getFields().get("closedDays").getTranslation()).isEqualTo("AI");
        assertThat(r.getWebsiteUrl()).as("URL은 번역하지 않음").isEqualTo("https://royal.khs.go.kr/");
        assertThat(trans.get("INTRO_REST_DATE|en|AI")).containsExactly("매주 화요일", "Closed on Tuesdays");
        server.verify(); // 조사(Gemini 검색) 호출 없음
    }

    @Test
    @DisplayName("[7] 수동 번역이 있으면 저장된 AI 번역보다 우선하고, AI 저장이 수동 번역 행을 덮어쓰지 않는다")
    void manualWins() {
        when(mapper.selectIntro("126508", "ko")).thenReturn(cachedKo());
        markNotFound("INTRO_USE_FEE", "INTRO_PARKING_FEE", "INTRO_INFO_CENTER");
        trans.put("INTRO_REST_DATE|en|AI", new String[]{"매주 화요일", "Tuesday off (AI)"});
        trans.put("INTRO_REST_DATE|en|MANUAL", new String[]{"매주 화요일", "Closed every Tuesday"});

        TourAiDetailData r = service.usageInfo("126508", "en", translator(Map.of("INTRO_PARKING", "Available")));

        assertThat(r.getClosedDays()).isEqualTo("Closed every Tuesday");
        assertThat(r.getFields().get("closedDays").getTranslation()).isEqualTo("MANUAL");
        assertThat(trans.get("INTRO_REST_DATE|en|MANUAL")[1]).isEqualTo("Closed every Tuesday");
        verify(mapper, never()).upsertTranslation(any(), any(), any(), eq("MANUAL"), any(), any());
    }

    @Test
    @DisplayName("[9] 국문 원문이 바뀌면(해시 불일치) 예전 번역을 쓰지 않고 다시 번역한다")
    void sourceChangedRetranslates() {
        TourIntroData ko = cachedKo();
        ko.setRestDate("매주 월요일");
        when(mapper.selectIntro("126508", "ko")).thenReturn(ko);
        markNotFound("INTRO_USE_FEE", "INTRO_PARKING_FEE", "INTRO_INFO_CENTER");
        trans.put("INTRO_REST_DATE|en|MANUAL", new String[]{"매주 화요일", "Closed on Tuesdays"});

        TourAiDetailData r = service.usageInfo("126508", "en",
                translator(Map.of("INTRO_REST_DATE", "Closed on Mondays", "INTRO_PARKING", "Available")));

        assertThat(r.getClosedDays()).isEqualTo("Closed on Mondays");
        assertThat(trans.get("INTRO_REST_DATE|en|AI")).containsExactly("매주 월요일", "Closed on Mondays");
    }

    @Test
    @DisplayName("해당 언어 TourAPI 공식 값이 최우선, 공식 값이 빈 항목만 국문을 번역한다. 공식 개요는 TOURAPI 번역으로 저장")
    void officialLanguageFirst() {
        when(mapper.selectIntro("126508", "ko")).thenReturn(cachedKo());
        markNotFound("INTRO_USE_FEE", "INTRO_PARKING_FEE", "INTRO_INFO_CENTER");
        when(mapper.langContentId("126508", "en")).thenReturn("264337");
        trans.put("INTRO_REST_DATE|en|MANUAL", new String[]{"매주 화요일", "Closed every Tuesday"});
        server.expect(requestTo(containsString("EngService2/detailCommon2"))).andRespond(withSuccess(EN_COMMON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(allOf(containsString("EngService2/detailIntro2"), containsString("contentTypeId=76"))))
                .andRespond(withSuccess(EN_INTRO, MediaType.APPLICATION_JSON));

        TourAiDetailData r = service.usageInfo("126508", "en", translator(Map.of("INTRO_PARKING", "Available (240 cars)")));

        assertThat(r.getOperatingHours()).isEqualTo("09:00-18:00");
        assertThat(r.getClosedDays()).isEqualTo("Tuesdays");
        assertThat(r.getFields().get("closedDays").getTranslation()).isEqualTo("OFFICIAL");
        assertThat(r.getInfoCenter()).isEqualTo("+82-2-3700-3900");
        assertThat(r.getParkingInfo()).isEqualTo("Available (240 cars)");
        // 공식 값이 우선 표시돼도 저장된 수동 번역은 그대로 남는다
        assertThat(trans.get("INTRO_REST_DATE|en|MANUAL")[1]).isEqualTo("Closed every Tuesday");
        assertThat(trans.get("TOUR_CONTENT|en|TOURAPI")).containsExactly("경복궁은 조선의 법궁이다.", "Gyeongbokgung Palace was built in 1395.");
        server.verify();
    }

    @Test
    @DisplayName("AI 번역 실패(예외/한글 그대로)는 저장하지 않고 국문 원문(KO_FALLBACK)으로 보여준다 — 조사 실패와 별개")
    void translationFailureShowsKorean() {
        when(mapper.selectIntro("126508", "ko")).thenReturn(cachedKo());
        markNotFound("INTRO_USE_FEE", "INTRO_PARKING_FEE", "INTRO_INFO_CENTER");

        TourAiDetailData r = service.usageInfo("126508", "ja", translator(Map.of("INTRO_REST_DATE", "매주 화요일")));
        assertThat(r.getClosedDays()).isEqualTo("매주 화요일");
        assertThat(r.getFields().get("closedDays").getTranslation()).isEqualTo("KO_FALLBACK");
        assertThat(r.getSource()).isEqualTo("TOURAPI_KO");

        TourAiDetailData r2 = service.usageInfo("126508", "ja", (t, l) -> { throw new RuntimeException("quota"); });
        assertThat(r2.getClosedDays()).isEqualTo("매주 화요일");
        assertThat(trans).isEmpty();
    }

    @Test
    @DisplayName("다른 요청이 같은 관광지 이용정보를 번역 중이면 AI 번역을 중복 호출하지 않는다")
    void translationDedupe() {
        when(mapper.selectIntro("126508", "ko")).thenReturn(cachedKo());
        markNotFound("INTRO_USE_FEE", "INTRO_PARKING_FEE", "INTRO_INFO_CENTER");
        assertThat(translationService.tryClaim("126508", "INTRO", "en")).isTrue();

        service.usageInfo("126508", "en", translator(Map.of("INTRO_REST_DATE", "Tuesdays")));

        assertThat(aiTranslateCalls.get()).isZero();
    }

    @Test
    @DisplayName("번역 저장이 실패해도 이번 응답에는 번역값을 보여준다")
    void translationSaveFailure() {
        when(mapper.selectIntro("126508", "ko")).thenReturn(cachedKo());
        markNotFound("INTRO_USE_FEE", "INTRO_PARKING_FEE", "INTRO_INFO_CENTER");
        when(mapper.upsertTranslation(any(), any(), any(), any(), any(), any())).thenThrow(new RuntimeException("db"));

        TourAiDetailData r = service.usageInfo("126508", "en",
                translator(Map.of("INTRO_REST_DATE", "Tuesdays", "INTRO_PARKING", "Available")));

        assertThat(r.getClosedDays()).isEqualTo("Tuesdays");
    }

    @Test
    @DisplayName("[성능] AI 번역이 늦으면(예: 15초 타임아웃) 화면은 기다리지 않고 국문으로 응답하고, 번역은 백그라운드에서 끝나 저장된다")
    void slowTranslationDoesNotBlock() throws Exception {
        when(mapper.selectIntro("126508", "ko")).thenReturn(cachedKo());
        markNotFound("INTRO_USE_FEE", "INTRO_PARKING_FEE", "INTRO_INFO_CENTER");
        service.translateExecutor = java.util.concurrent.ForkJoinPool.commonPool();
        service.translateWaitMillis = 100;
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        BiFunction<Map<String, String>, String, Map<String, String>> slow = (texts, lang) -> {
            aiTranslateCalls.incrementAndGet();
            try { Thread.sleep(600); } catch (InterruptedException ignored) { }
            return Map.of("INTRO_REST_DATE", "Closed on Tuesdays", "INTRO_PARKING", "Available");
        };

        long start = System.currentTimeMillis();
        TourAiDetailData first = service.usageInfo("126508", "en", slow);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isLessThan(500);
        assertThat(first.getClosedDays()).isEqualTo("매주 화요일");
        assertThat(first.getFields().get("closedDays").getTranslation()).isEqualTo("KO_FALLBACK");

        // 백그라운드 번역이 끝나 저장되고 잠금이 풀릴 때까지 대기
        for (int i = 0; i < 50 && !(trans.containsKey("INTRO_REST_DATE|en|AI") && translationService.tryClaim("126508", "INTRO", "en")); i++) Thread.sleep(50);
        translationService.release("126508", "INTRO", "en");
        assertThat(trans.get("INTRO_REST_DATE|en|AI")).containsExactly("매주 화요일", "Closed on Tuesdays");

        TourAiDetailData second = service.usageInfo("126508", "en", slow);
        assertThat(second.getClosedDays()).isEqualTo("Closed on Tuesdays");
        assertThat(aiTranslateCalls.get()).as("두 번째 조회는 AI 번역 없음").isEqualTo(1);
    }

    // ───────────── 캐시 / 파싱 ─────────────

    @Test
    @DisplayName("TourAPI 캐시가 신선하면 TourAPI를 다시 부르지 않는다")
    void cacheHit() {
        when(mapper.selectIntro("126508", "ko")).thenReturn(cachedKo());
        markNotFound("INTRO_USE_FEE", "INTRO_PARKING_FEE", "INTRO_INFO_CENTER");

        assertThat(service.usageInfo("126508", "ko", null).getClosedDays()).isEqualTo("매주 화요일");
        server.verify();
        verify(mapper, never()).upsertIntro(any());
    }

    @Test
    @DisplayName("TourAPI 장애라도 오래된 캐시가 있으면 그걸 보여준다")
    void apiFailureUsesStaleCache() {
        TourIntroData stale = cachedKo();
        stale.setFetchDt(LocalDateTime.now().minusDays(60));
        when(mapper.selectIntro("126508", "ko")).thenReturn(stale);
        markNotFound("INTRO_USE_FEE", "INTRO_PARKING_FEE", "INTRO_INFO_CENTER");
        server.expect(ExpectedCount.manyTimes(), requestTo(containsString("KorService2"))).andRespond(withServerError());

        assertThat(service.usageInfo("126508", "ko", null).getClosedDays()).isEqualTo("매주 화요일");
    }

    @Test
    @DisplayName("관광타입별 항목명(usetimeculture/usefee/parkingfee/opentimefood...)을 같은 화면 항목으로 모은다")
    void parseVariants() throws Exception {
        var om = new ObjectMapper();
        var intro = om.readTree("""
                {"usetimeculture":"10:00~19:00","restdateculture":"1월 1일","usefee":"성인 3,000원",
                 "parkingculture":"있음","parkingfee":"무료","infocenterculture":"02-111-2222","spendtime":"1시간"}""");
        TourIntroData d = TourIntroService.parse(intro, null);
        assertThat(d.getUseTime()).isEqualTo("10:00~19:00");
        assertThat(d.getRestDate()).isEqualTo("1월 1일");
        assertThat(d.getUseFee()).isEqualTo("성인 3,000원");
        assertThat(d.getParking()).isEqualTo("있음");
        assertThat(d.getParkingFee()).isEqualTo("무료");
        assertThat(d.getInfoCenter()).isEqualTo("02-111-2222");

        assertThat(TourIntroService.parse(om.readTree("{\"opentimefood\":\"11:00~21:00\"}"), null).getUseTime()).isEqualTo("11:00~21:00");
        TourIntroData f = TourIntroService.parse(om.readTree("{\"playtime\":\"18:00~22:00\",\"usetimefestival\":\"무료\"}"), null);
        assertThat(f.getUseTime()).isEqualTo("18:00~22:00");
        assertThat(f.getUseFee()).isEqualTo("무료");
    }

    @Test
    @DisplayName("홈페이지 HTML에서 URL만 뽑는다")
    void extractUrl() {
        assertThat(TourIntroService.extractUrl("<a href=\"http://www.a.kr\" target=\"_blank\">www.a.kr</a>")).isEqualTo("http://www.a.kr");
        assertThat(TourIntroService.extractUrl("www.b.kr")).isEqualTo("https://www.b.kr");
        assertThat(TourIntroService.extractUrl("")).isNull();
        assertThat(TourIntroService.extractUrl("홈페이지 없음")).isNull();
    }

    @Test
    @DisplayName("출처 검증: 검색 근거 도메인과 같거나 하위 도메인일 때만 통과")
    void verifySource() {
        List<String[]> ev = List.<String[]>of(new String[]{"https://vertexaisearch.cloud.google.com/r/1", "www.royal.khs.go.kr"});
        assertThat(TourInfoResearchService.verify("INTRO_USE_FEE", "3,000원", "https://royal.khs.go.kr/a", ev)).isNotNull();
        assertThat(TourInfoResearchService.verify("INTRO_USE_FEE", "3,000원", "https://evil.com/royal.khs.go.kr", ev)).isNull();
        assertThat(TourInfoResearchService.verify("INTRO_HOMEPAGE", "공식 홈페이지", "https://royal.khs.go.kr", ev)).as("홈페이지 값은 URL이어야").isNull();
        assertThat(TourInfoResearchService.verify("INTRO_USE_FEE", "알 수 없음", "https://royal.khs.go.kr/a", ev)).isNull();
    }
}
