package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.*;
import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.mapper.TourListMapper;
import com.example.backend.trma.service.TourListService;
import com.example.backend.trma.util.AiErrorUtil;
import com.example.backend.trma.util.AiJsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourListServicelmpl implements TourListService {

    private final TourListMapper tourListMapper;
    private final RestClient restClient;

    //사용자 정보 조회
    public TourSearchResponse tourSearch(TourSearchRequest request) {

        try {
            if(request.getPage() == 0){
                request.setPage(1);
            }
            int offset = (request.getPage() - 1) * 10 ;
            request.setOffset(offset);

            List<TourSearchData> tourSearch = tourListMapper.tourSearch(request);
            applySearchTranslations(tourSearch, request.getLang());
            applySearchAddressTranslations(tourSearch, request.getLang());
            return new TourSearchResponse(
                    true,
                    200,
                    "SUCCESS",
                    "관광지 정보를 정상적으로 조회했습니다.",
                    "/tourList/tourSearch",
                    "",
                    tourSearch
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);

            return new TourSearchResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/tourList/tourSearch",
                    "",
                    null
            );
        }
    }

    @Value("${gemini.api.url}")
    private String url;

    @Value("${gemini.api.key}")
    private String apiKey;

    //AI 관광지 추천
    public TourAiSearchResponse tourAiSearch(TourAiSearchRequest request) {
        try {
            String keyword = request.getKeyword();
            String cateCd = request.getCateCd();

            String prompt = "" +
                    "당신은 대한민국 여행 전문 AI입니다.\n" +
                    "다음 규칙을 반드시 지켜주세요.\n" +
                    "[규칙]\n" +
                    "1. 반드시 제공된 관광지 목록 안에서만 추천하세요.\n" +
                    "2. 목록에 없는 관광지는 절대로 추천하지 마세요.\n" +
                    "3. 추천 이유는 100자 이내로 작성하세요.\n" +
                    "4. 반드시 JSON 형식으로만 응답하세요.\n" +
                    "5. JSON 이외의 설명이나 문장은 작성하지 마세요.\n";

            if ((keyword != null && !keyword.isBlank()) ||
                    (cateCd != null && !cateCd.isBlank())) {

                prompt = prompt + ("6. 사용자 조건을 가장 잘 만족하는 관광지 3곳만 추천하세요.\n") +
                        ("[사용자 조건]\n");

                if (keyword != null && !keyword.isBlank()) {
                    prompt = prompt + ("검색어: ") + keyword + ("\n");
                }

                if (cateCd != null && !cateCd.isBlank()) {
                    prompt = prompt + ("여행 테마: ") + cateCd + ("\n");
                }
            }

            TourSearchRequest request2 = new TourSearchRequest();
            request2.setKeyword(request.getKeyword());
            request2.setCateCd(request.getCateCd());

            List<TourSearchData> tourSearch = tourListMapper.tourSearch(request2);

            // 검색어+테마 조합에 맞는 관광지가 없으면 검색어를 빼고 테마만으로, 그래도
            // 없으면 테마도 빼고 평점 높은 순으로 다시 찾는다. 예전에는 여기서 DB에
            // 존재하지 않는 가짜 관광지 목록을 프롬프트에 넣었는데, Gemini가 그 가짜
            // ID를 추천하면 이후 실제 DB 조회에서 매칭되는 게 없어 이름/주소 등이 전부
            // 비어 보이는 문제가 있었다. 항상 실제 DB에 있는 관광지만 후보로 준다.
            if ((tourSearch == null || tourSearch.isEmpty())
                    && request2.getKeyword() != null && !request2.getKeyword().isBlank()) {
                TourSearchRequest cateOnly = new TourSearchRequest();
                cateOnly.setCateCd(request.getCateCd());
                tourSearch = tourListMapper.tourSearch(cateOnly);
            }
            if (tourSearch == null || tourSearch.isEmpty()) {
                tourSearch = tourListMapper.tourSearch(new TourSearchRequest());
            }

            StringBuilder tourListPrompt = new StringBuilder();

            prompt = prompt + "[관광지 목록]\n";

            int no = 1;

            for (TourSearchData tour : tourSearch) {
                tourListPrompt.append("""
                        [관광지 번호 %03d]
                        관광지ID : %s
                        관광지명 : %s
                        도로명주소 : %s
                        카테고리 : %s
                        """.formatted(
                        no++,
                        tour.getTourId(),
                        tour.getTourNm(),
                        tour.getRoadAddr(),
                        tour.getCateNm()
                ));
            }

            prompt = prompt + tourListPrompt;

                prompt = prompt + "응답 예시\n" +
                        "{\n" +
                        "  \"recommendations\":[\n" +
                        "    {\n" +
                        "      \"tourId\":\"T0001\",\n" +
                        "      \"score\":\"4.8\",\n" +
                        "      \"reason\":\"자연경관이 뛰어나며 가족과 함께 산책하기 좋습니다.\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"tourId\":\"T0005\",\n" +
                        "      \"score\":\"4.3\",\n" +
                        "      \"reason\":\"아이들과 체험하기 좋은 실내 관광지입니다.\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"tourId\":\"T0012\",\n" +
                        "      \"score\":\"3.7\",\n" +
                        "      \"reason\":\"야경이 아름답고 커플 여행에 적합합니다.\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

            GeminiRequest geminiRequest = new GeminiRequest(List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))));

            GeminiResponse response = callGemini(geminiRequest);

            String aiResult = "";

            if (response != null
                    && response.candidates() != null
                    && !response.candidates().isEmpty()) {

                aiResult = response.candidates()
                        .get(0)
                        .content()
                        .parts()
                        .get(0)
                        .text();
            }

            System.out.println(aiResult);

            ObjectMapper objectMapper = new ObjectMapper();
            GeminiData recommend =
                    objectMapper.readValue(aiResult, GeminiData.class);

            List<TourAiSearchData> tourAiSearchData = new ArrayList<>();

            for (GeminiData.Recommendation item : recommend.getRecommendations()) {

                TourSearchData tourInfo = tourListMapper.tourInfo(item.getTourId());

                TourAiSearchData data = new TourAiSearchData();

                data.setTourId(item.getTourId());
                if (tourInfo != null) {
                    data.setTourNm(tourInfo.getTourNm());
                    data.setSidoCd(tourInfo.getSidoCd());
                    data.setSidoNm(tourInfo.getSidoNm());
                    data.setSggCd(tourInfo.getSggCd());
                    data.setSggNm(tourInfo.getSggNm());
                    data.setRoadAddr(tourInfo.getRoadAddr());
                    data.setDetailAddr(tourInfo.getDetailAddr());
                    data.setZipCd(tourInfo.getZipCd());
                    data.setCateCd(tourInfo.getCateCd());
                    data.setCateNm(tourInfo.getCateNm());
                }
                data.setScore(item.getScore());
                data.setReason(item.getReason());

                tourAiSearchData.add(data);
            }

            return new TourAiSearchResponse(
                    true,
                    200,
                    "SUCCESS",
                    "AI 관광지 추천 정보를 정상적으로 조회했습니다.",
                    "/tourList/tourAiSearch",
                    "",
                    tourAiSearchData
            );

        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);

            if (AiErrorUtil.isAiOverloaded(e)) {
                return new TourAiSearchResponse(
                        false,
                        503,
                        AiErrorUtil.CODE,
                        AiErrorUtil.MESSAGE,
                        "/tourList/tourAiSearch",
                        "",
                        null
                );
            }

            return new TourAiSearchResponse(
                    false,
                    500,
                    "FAIL",
                    e.getMessage(),
                    "/tourList/tourAiSearch",
                    "",
                    null
            );
        }
    }

    //관광지 상세조회(기본)
    public TourDetailResponse tourDetail(TourDetailRequest request) {

        try {
            TourDetailData tourDetail = tourListMapper.tourDetail(request.getTourId(), request.getLang());
            applyDetailTranslation(tourDetail, request.getLang());

            return new TourDetailResponse(
                    true,
                    200,
                    "SUCCESS",
                    "관광지 상세조회(기본)를 정상적으로 조회했습니다.",
                    "/tourList/tourDetail",
                    "",
                    tourDetail
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new TourDetailResponse(
                    false,
                    500,
                    "FAIL",
                    e.getMessage(),
                    "/tourList/tourDetail",
                    "",
                    null
            );
        }
    }

    //관광지 상세조회(AI)
    public TourAiDetailResponse tourAiDetail(TourAiDetailRequest request) {

        String langCd = (request.getLang() == null || request.getLang().isBlank()) ? "ko" : request.getLang();

        try {
            StringBuilder tourListPrompt = new StringBuilder();
            TourDetailData tourDetail = tourListMapper.tourDetail(request.getTourId(), request.getLang());

            String prompt = "당신은 대한민국 여행 전문 AI입니다.\n";


            tourListPrompt.append("""
                            [대상 관광지]
                            관광지ID : %s
                            관광지명 : %s
                            도로명주소 : %s
                            카테고리 : %s
                            """.formatted(
                    tourDetail.getTourId(),
                    tourDetail.getTourNm(),
                    tourDetail.getRoadAddr(),
                    tourDetail.getCateNm()
            ));

            String aiDetailLangLabel = "ja".equals(request.getLang()) ? "일본어"
                    : "en".equals(request.getLang()) ? "영어"
                    : "한국어";

            prompt = prompt + "[요청 사항]\n" +
                    "1. 해당 관광지의 운영시간, 휴무일, 입장료, 공식/관련 홈페이지 URL, 주차 정보(가능 여부 및 요금)를 정확하게 작성해줘.\n" +
                    "2. 정보가 불확실하거나 수집할 수 없는 항목은 null로 표시해줘.\n" +
                    "3. 부연 설명이나 인삿말은 모두 제외하고, 오직 순수한 JSON 데이터만 반환해줘.\n" +
                    "4. JSON의 키 이름은 아래 형식 그대로 영어로 유지하되, operatingHours/closedDays/admissionFeeDetails/parkingFeeInfo/lastUpdatedNote 값은 반드시 " + aiDetailLangLabel + "로 작성해줘. (websiteUrl은 번역하지 말고 그대로)\n" +
                    "\n" +
                    "[JSON 반환 형식]\n" +
                    "{\n" +
                    "  \"place_name\": \"관광지 이름\",\n" +
                    "  \"address\": \"주소\",\n" +
                    "  \"operatingHours\": \"운영시간 정보 (예: 09:00 - 18:00, 입장마감 17:00)\",\n" +
                    "  \"closedDays\": \"휴무일 정보 (예: 매주 월요일, 명절 당일)\",\n" +
                    "  \"admissionFeeIsFree\": \"false\",\n" +
                    "  \"admissionFeeDetails\": \"입장료 상세 (예: 성인 3,000원, 청소년 1,500원 등)\",\n" +
                    "  \"websiteUrl\": \"공식 홈페이지 URL (없을 경우 관련 정보 URL)\",\n" +
                    "  \"parkingAvailable\": \"true\",\n" +
                    "  \"parkingFeeInfo\": \"주차 요금 및 정보 (예: 소형 2,000원/시간, 무료 등)\",\n" +
                    "  \"lastUpdatedNote\": \"특이사항이나 참고사항 (예: 야간개장 시즌 운영 등)\"\n" +
                    "}";

            GeminiRequest geminiRequest = new GeminiRequest(List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))));

            GeminiResponse response = callGemini(geminiRequest);

            String aiResult = "";

            if (response != null
                    && response.candidates() != null
                    && !response.candidates().isEmpty()) {

                aiResult = response.candidates()
                        .get(0)
                        .content()
                        .parts()
                        .get(0)
                        .text();
            }

            System.out.println(aiResult);

            ObjectMapper objectMapper = new ObjectMapper();
            GeminiData.Recommendation3 recommend =
                    objectMapper.readValue(AiJsonUtil.extractJson(aiResult), GeminiData.Recommendation3.class);

            TourAiDetailData tourAiDetail = new TourAiDetailData();

            tourAiDetail.setOperatingHours(recommend.getOperatingHours());
            tourAiDetail.setClosedDays(recommend.getClosedDays());
            tourAiDetail.setAdmissionFeeIsFree(recommend.getAdmissionFeeIsFree());
            tourAiDetail.setAdmissionFeeDetails(recommend.getAdmissionFeeDetails());
            tourAiDetail.setWebsiteUrl(recommend.getWebsiteUrl());
            tourAiDetail.setParkingAvailable(recommend.getParkingAvailable());
            tourAiDetail.setParkingFeeInfo(recommend.getParkingFeeInfo());
            tourAiDetail.setLastUpdatedNote(recommend.getLastUpdatedNote());

            // AI 응답이 정상일 때마다 캐시를 최신 상태로 갱신해둔다. 이렇게 해야 다음에
            // AI가 끊겼을 때 지금 이 결과를 대신 보여줄 수 있다.
            try {
                tourListMapper.upsertTourAiInfo(request.getTourId(), langCd, tourAiDetail);
            } catch (Exception cacheEx) {
                log.warn("관광지 AI 이용정보 캐시 저장에 실패했습니다. tourId={}", request.getTourId(), cacheEx);
            }

            return new TourAiDetailResponse(
                    true,
                    200,
                    "SUCCESS",
                    "관광지 상세조회(AI)를 정상적으로 조회했습니다.",
                    "/tourList/tourAiDetail",
                    "",
                    tourAiDetail
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);

            // AI 호출/응답 파싱이 실패해도, 이전에 저장해둔 이용정보가 있으면 그걸로 대신 보여준다.
            TourAiDetailData cached = tourListMapper.selectTourAiInfo(request.getTourId(), langCd);
            if (cached != null) {
                return new TourAiDetailResponse(
                        true,
                        200,
                        "SUCCESS",
                        "AI 응답에 실패해 이전에 저장된 이용정보를 보여드려요.",
                        "/tourList/tourAiDetail",
                        "",
                        cached
                );
            }

            if (AiErrorUtil.isAiOverloaded(e)) {
                return new TourAiDetailResponse(
                        false,
                        503,
                        AiErrorUtil.CODE,
                        AiErrorUtil.MESSAGE,
                        "/tourList/tourAiDetail",
                        "",
                        null
                );
            }

            return new TourAiDetailResponse(
                    false,
                    500,
                    "FAIL",
                    e.getMessage(),
                    "/tourList/tourAiDetail",
                    "",
                    null
            );
        }
    }

    //관광지 상세조회(리뷰)
    public TourDetailReviewResponse tourDetailReview(TourDetailReviewRequest request) {

        try {
            if (request.getPage() == 0) {
                request.setPage(1);
            }
            int offset = (request.getPage() - 1) * 10;
            List<TourDetailReviewData> tourDetailReview = tourListMapper.tourDetailReview(request.getTourId(), offset);
            applyReviewTranslations(tourDetailReview, request.getLang());

            return new TourDetailReviewResponse(
                    true,
                    200,
                    "SUCCESS",
                    "관광지 상세조회(리뷰)를 정상적으로 조회했습니다.",
                    "/tourList/tourDetailReview",
                    "",
                    tourDetailReview
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new TourDetailReviewResponse(
                    false,
                    500,
                    "FAIL",
                    e.getMessage(),
                    "/tourList/tourDetailReview",
                    "",
                    null
            );
        }
    }

    //관광지 후기 등록
    public CreateTourReviewResponse createTourReview(CreateTourReviewRequest request, String userId) {

        try {
            String imgUrls = request.getImageUrls() == null ? null : String.join(",", request.getImageUrls());
            String reviewId = "RVT" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            tourListMapper.insertTourReview(reviewId, request, imgUrls, userId);

            return new CreateTourReviewResponse(
                    true,
                    200,
                    "SUCCESS",
                    "후기 등록이 완료되었습니다.",
                    "/tourList/tourDetailReview",
                    ""
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new CreateTourReviewResponse(
                    false,
                    500,
                    "FAIL",
                    "후기 등록 중 오류가 발생했습니다.",
                    "/tourList/tourDetailReview",
                    ""
            );
        }
    }

    //AI 일정 추천
    public AiScheduleResponse aiSchedule(AiScheduleRequest request) {

        try {
            int dayCount = Math.max(1, request.getDayCount());

            TourSearchRequest searchRequest = new TourSearchRequest();
            searchRequest.setCateCd(request.getCateCd());
            searchRequest.setKeyword(request.getKeyword());

            List<TourSearchData> tourList = tourListMapper.tourSearch(searchRequest);

            // 검색어+테마 조합에 맞는 관광지가 없으면 테마만으로, 그래도 없으면 테마도 빼고
            // 평점 높은 순으로 다시 찾는다(항상 실제 DB에 있는 관광지만 후보로 준다).
            // 예전에는 키워드만 "여행"으로 바꿔 재시도했는데 테마(cateCd) 필터가 그대로
            // 남아있어서 "여행"이라는 단어와 그 테마를 동시에 만족하는 관광지가 없으면
            // 여전히 0건이었고, 그 상태로 빈 관광지 목록을 그대로 Gemini에 보내고 있었다.
            if (tourList == null || tourList.isEmpty()) {
                TourSearchRequest cateOnly = new TourSearchRequest();
                cateOnly.setCateCd(request.getCateCd());
                tourList = tourListMapper.tourSearch(cateOnly);
            }
            if (tourList == null || tourList.isEmpty()) {
                tourList = tourListMapper.tourSearch(new TourSearchRequest());
            }

            List<AiScheduleExistingItem> existingItems = request.getExistingItems() != null
                    ? request.getExistingItems()
                    : List.of();
            // 이미 일정에 들어가 있는 관광지는 추천 후보에서 빼서, 같은 곳을 또 추천하지
            // 않게 한다.
            if (!existingItems.isEmpty()) {
                java.util.Set<String> existingTourIds = new java.util.HashSet<>();
                for (AiScheduleExistingItem item : existingItems) {
                    existingTourIds.add(item.getTourId());
                }
                tourList = tourList.stream()
                        .filter(tour -> !existingTourIds.contains(tour.getTourId()))
                        .toList();
            }

            StringBuilder tourListPrompt = new StringBuilder();
            int no = 1;
            for (TourSearchData tour : tourList) {
                tourListPrompt.append("""
                        [관광지 번호 %03d]
                        관광지ID : %s
                        관광지명 : %s
                        카테고리 : %s
                        """.formatted(no++, tour.getTourId(), tour.getTourNm(), tour.getCateNm()));
            }

            // 사용자가 화면에서 이미 직접 추가해둔 일정이 있으면, 그 일정은 그대로 두고
            // 빈 시간대만 채우도록 Gemini에게 알려준다.
            StringBuilder existingSchedulePrompt = new StringBuilder();
            if (!existingItems.isEmpty()) {
                for (AiScheduleExistingItem item : existingItems) {
                    TourSearchData tourInfo = tourListMapper.tourInfo(item.getTourId());
                    String tourNm = tourInfo != null ? tourInfo.getTourNm() : item.getTourId();
                    existingSchedulePrompt.append("%s일차 %s : %s\n"
                            .formatted(item.getDay(), item.getTime(), tourNm));
                }
            }

            StringBuilder conditionPrompt = new StringBuilder();
            if (request.getMoimStartDt() != null && !request.getMoimStartDt().isBlank()) {
                conditionPrompt.append("여행 기간: ").append(request.getMoimStartDt());
                if (request.getMoimEndDt() != null && !request.getMoimEndDt().isBlank()) {
                    conditionPrompt.append(" ~ ").append(request.getMoimEndDt());
                }
                conditionPrompt.append(" (총 ").append(dayCount).append("일)\n");
            } else {
                conditionPrompt.append("여행 기간: 총 ").append(dayCount).append("일\n");
            }
            if (request.getCateNms() != null && !request.getCateNms().isBlank()) {
                conditionPrompt.append("관심 테마/여행 목적: ").append(request.getCateNms()).append("\n");
            }
            if (request.getMaxMember() != null && request.getMaxMember() > 0) {
                conditionPrompt.append("동행 인원: ").append(request.getMaxMember()).append("명\n");
            }

            boolean hasExisting = !existingItems.isEmpty();

            String prompt = "당신은 대한민국 여행 일정 플래너입니다.\n"
                    + "다음 관광지 목록 중에서만 골라, 아래 조건에 맞는 " + dayCount + "일 여행 일정을 만들어주세요.\n"
                    + "[여행 조건]\n" + conditionPrompt
                    + (hasExisting
                            ? "[이미 확정된 일정 - 그대로 유지, 수정/삭제하지 말 것]\n" + existingSchedulePrompt
                            : "")
                    + "[규칙]\n"
                    + "1. 반드시 제공된 목록에 있는 관광지ID만 사용하세요.\n"
                    + "2. 하루에 2~3개의 관광지를 배정하세요.\n"
                    + "3. 시간은 09:00~18:00 사이로, 이동 시간을 고려해 배정하세요.\n"
                    + "4. 하루 안에서는 시간 순서대로 정렬하세요.\n"
                    + "5. 관심 테마/여행 목적과 동행 인원을 고려해 어울리는 관광지 위주로 배정하세요.\n"
                    + (hasExisting
                            ? "6. 위 [이미 확정된 일정]과 겹치지 않는 시간대만 추가로 채우세요. 이미 확정된"
                                    + " 일정 자체는 응답에 절대 포함하지 말고, 새로 추가할 항목만 응답하세요.\n"
                                    + "7. 반드시 JSON 형식으로만 응답하세요. 다른 설명은 절대 포함하지 마세요.\n"
                            : "6. 반드시 JSON 형식으로만 응답하세요. 다른 설명은 절대 포함하지 마세요.\n")
                    + "[응답 형식]\n"
                    + "{\"recommendations4\":[{\"day\":1,\"time\":\"10:00\",\"tourId\":\"T0001\"}]}\n"
                    + "[관광지 목록]\n" + tourListPrompt;

            GeminiRequest geminiRequest = new GeminiRequest(List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))));

            GeminiResponse response = callGemini(geminiRequest);

            String aiResult = "";
            if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                aiResult = response.candidates().get(0).content().parts().get(0).text();
            }

            ObjectMapper objectMapper = new ObjectMapper();
            GeminiData recommend = objectMapper.readValue(AiJsonUtil.extractJson(aiResult), GeminiData.class);

            List<AiScheduleItemData> schedule = new ArrayList<>();
            if (recommend.getRecommendations4() != null) {
                for (GeminiData.Recommendation4 item : recommend.getRecommendations4()) {
                    TourSearchData tourInfo = tourListMapper.tourInfo(item.getTourId());
                    if (tourInfo == null) continue;

                    AiScheduleItemData data = new AiScheduleItemData();
                    data.setDay(item.getDay());
                    data.setTime(item.getTime());
                    data.setTourId(item.getTourId());
                    data.setTourNm(tourInfo.getTourNm());
                    data.setFirstImage(tourInfo.getFirstImage());
                    data.setRoadAddr(tourInfo.getRoadAddr());
                    schedule.add(data);
                }
            }

            return new AiScheduleResponse(
                    true,
                    200,
                    "SUCCESS",
                    "AI 일정 추천을 정상적으로 생성했습니다.",
                    "/tourList/aiSchedule",
                    "",
                    schedule
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);

            if (AiErrorUtil.isAiOverloaded(e)) {
                return new AiScheduleResponse(
                        false,
                        503,
                        AiErrorUtil.CODE,
                        AiErrorUtil.MESSAGE,
                        "/tourList/aiSchedule",
                        "",
                        null
                );
            }

            return new AiScheduleResponse(
                    false,
                    500,
                    "FAIL",
                    "AI 일정 추천 중 오류가 발생했습니다.",
                    "/tourList/aiSchedule",
                    "",
                    null
            );
        }
    }

    //사용자 관광지 등록(소모임 생성 시 직접 입력)
    @Override
    public CustomTourResponse registerCustomTour(CustomTourRequest request, String userId) {

        try {
            String tourId = "UGC" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
            tourListMapper.insertCustomTour(tourId, request, userId);

            CustomTourData data = new CustomTourData();
            data.setTourId(tourId);
            data.setTourNm(request.getTourNm());
            data.setRoadAddr(request.getRoadAddr());

            return new CustomTourResponse(
                    true,
                    200,
                    "SUCCESS",
                    "관광지가 등록되었습니다.",
                    "/tourList/customTour",
                    "",
                    data
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new CustomTourResponse(
                    false,
                    500,
                    "FAIL",
                    "관광지 등록 중 오류가 발생했습니다.",
                    "/tourList/customTour",
                    "",
                    null
            );
        }
    }

    //관광지 이름 일괄 번역(캐시 우선, 없으면 번역 후 캐시에 저장)
    @Override
    public Map<String, String> translateTourNames(List<String> tourIds, String lang) {
        if (!"en".equals(lang) && !"ja".equals(lang)) return Map.of();
        if (tourIds == null || tourIds.isEmpty()) return Map.of();

        List<String> distinctIds = tourIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) return Map.of();

        List<TourSearchData> tours = tourListMapper.tourByIds(distinctIds, lang);
        applySearchTranslations(tours, lang);

        Map<String, String> result = new HashMap<>();
        for (TourSearchData tour : tours) {
            result.put(tour.getTourId(), tour.getTourNm());
        }
        return result;
    }

    //임의의 짧은 텍스트들을 key로 구분해 한 번에 번역. reviewId/reviewContent 필드를
    //각각 key/번역결과 용도로 재사용해서(REVIEW_ID 배치 번역과 동일한 모양) 새
    //GeminiData 타입을 늘리지 않는다.
    @Override
    public Map<String, String> translateFreeTexts(Map<String, String> textsByKey, String lang) {
        if (!"en".equals(lang) && !"ja".equals(lang)) return Map.of();
        if (textsByKey == null || textsByKey.isEmpty()) return Map.of();

        Map<String, String> nonBlank = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : textsByKey.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                nonBlank.put(entry.getKey(), entry.getValue());
            }
        }
        if (nonBlank.isEmpty()) return Map.of();

        try {
            StringBuilder listPrompt = new StringBuilder();
            for (Map.Entry<String, String> entry : nonBlank.entrySet()) {
                listPrompt.append("[reviewId %s]\n내용: %s\n\n".formatted(entry.getKey(), entry.getValue()));
            }

            String langLabel = "en".equals(lang) ? "영어" : "일본어";
            String prompt = "다음 텍스트들을 " + langLabel + "로 자연스럽게 번역해주세요.\n"
                    + "반드시 JSON 형식으로만 응답하고, 요청받은 reviewId를 그대로 포함해서 응답하세요.\n"
                    + "[응답 형식]\n"
                    + "{\"reviewTranslations\":[{\"reviewId\":\"key1\",\"reviewContent\":\"번역된 텍스트\"}]}\n"
                    + "[텍스트 목록]\n" + listPrompt;

            GeminiData result = callGeminiForTranslation(prompt);
            if (result == null || result.getReviewTranslations() == null) return Map.of();

            Map<String, String> translated = new HashMap<>();
            for (GeminiData.ReviewTranslationItem item : result.getReviewTranslations()) {
                if (item.getReviewId() != null && item.getReviewContent() != null && !item.getReviewContent().isBlank()) {
                    translated.put(item.getReviewId(), item.getReviewContent());
                }
            }
            return translated;
        } catch (Exception e) {
            log.warn("텍스트 번역에 실패했습니다. lang={}", lang, e);
            return Map.of();
        }
    }

    //일정별 교통편 추천
    @Override
    public TransportRecommendResponse transportRecommend(TransportRecommendRequest request) {

        try {
            List<TransportRecommendRequest.TransportStopInput> items = request.getItems();
            if (items == null || items.size() < 2) {
                return new TransportRecommendResponse(
                        true,
                        200,
                        "SUCCESS",
                        "이동할 구간이 없습니다.",
                        "/tourList/transportRecommend",
                        "",
                        new ArrayList<>()
                );
            }

            // 같은 날짜(day) 안에서 시간 순으로 연속된 관광지 쌍만 이동 구간으로 본다
            // (날짜가 바뀌는 지점은 실제 이동이 아니므로 제외)
            Map<Integer, List<TransportRecommendRequest.TransportStopInput>> byDay = new LinkedHashMap<>();
            for (TransportRecommendRequest.TransportStopInput item : items) {
                byDay.computeIfAbsent(item.getDay(), k -> new ArrayList<>()).add(item);
            }
            for (List<TransportRecommendRequest.TransportStopInput> dayItems : byDay.values()) {
                dayItems.sort((a, b) -> {
                    String ta = a.getTime() == null ? "" : a.getTime();
                    String tb = b.getTime() == null ? "" : b.getTime();
                    return ta.compareTo(tb);
                });
            }

            StringBuilder legPrompt = new StringBuilder();
            int legNo = 0;
            for (Map.Entry<Integer, List<TransportRecommendRequest.TransportStopInput>> entry : byDay.entrySet()) {
                List<TransportRecommendRequest.TransportStopInput> dayItems = entry.getValue();
                for (int i = 0; i < dayItems.size() - 1; i++) {
                    TransportRecommendRequest.TransportStopInput from = dayItems.get(i);
                    TransportRecommendRequest.TransportStopInput to = dayItems.get(i + 1);
                    legPrompt.append("""
                            [구간 번호 %d]
                            day : %d
                            출발 관광지ID : %s / 이름 : %s / 주소 : %s
                            도착 관광지ID : %s / 이름 : %s / 주소 : %s
                            """.formatted(
                            legNo++, entry.getKey(),
                            from.getTourId(), from.getTourNm(), from.getRoadAddr(),
                            to.getTourId(), to.getTourNm(), to.getRoadAddr()
                    ));
                }
            }

            if (legNo == 0) {
                return new TransportRecommendResponse(
                        true,
                        200,
                        "SUCCESS",
                        "이동할 구간이 없습니다.",
                        "/tourList/transportRecommend",
                        "",
                        new ArrayList<>()
                );
            }

            String legLangLabel = "en".equals(request.getLang()) ? "영어" : "ja".equals(request.getLang()) ? "일본어" : null;

            String prompt = "당신은 대한민국 대중교통과 도로 혼잡 패턴에 정통한 여행 이동 전문가입니다.\n"
                    + "다음은 하루 일정 안에서 연속으로 방문하는 관광지 구간 목록입니다.\n"
                    + "각 구간마다 두 관광지의 주소와 방문 시각을 바탕으로 가장 적절한 이동수단, 예상 소요시간(분), "
                    + "예상 비용(원), 환승 횟수를 추천하고, 해당 시각대의 예상 혼잡도와 지연 위험, "
                    + "혼잡할 경우의 대체 이동수단까지 함께 제시해주세요.\n"
                    + "[규칙]\n"
                    + "1. 이동수단은 지하철, 버스, 도보, 택시, 자가용/렌터카 중 실제 거리에 맞는 것으로 고르세요.\n"
                    + "2. 도보로 15분 이내인 거리는 도보를 우선 추천하세요.\n"
                    + "3. 정확한 수치를 모르더라도 주소 간 거리와 방문 시각(출퇴근 시간대, 주말 등)을 바탕으로 합리적인 추정치를 제시하세요.\n"
                    + "4. congestionLevel은 \"원활\", \"보통\", \"혼잡\" 중 하나로 답하세요.\n"
                    + "5. congestionLevel이 \"혼잡\"일 때만 delayRiskMinutes(예상 지연 분)와 alternativeMode(대체 이동수단), "
                    + "alternativeReason(대체를 추천하는 이유, 한 문장)을 채우고, 그 외에는 모두 null로 두세요.\n"
                    + "6. 반드시 JSON 형식으로만 응답하고, 요청받은 day와 관광지ID를 그대로 포함해서 응답하세요.\n"
                    + (legLangLabel != null
                        ? "7. mode, alternativeMode, congestionLevel 값은 반드시 지하철/버스/도보/택시/자가용/렌터카/원활/보통/혼잡 중 하나의 한국어 표기 그대로 쓰고(화면에서 별도로 번역합니다), alternativeReason만 " + legLangLabel + "로 작성해주세요.\n"
                        : "")
                    + "[응답 형식]\n"
                    + "{\"transportLegs\":[{\"day\":1,\"fromTourId\":\"T0001\",\"toTourId\":\"T0002\","
                    + "\"mode\":\"지하철\",\"durationMinutes\":20,\"cost\":1500,\"transferCount\":0,"
                    + "\"congestionLevel\":\"혼잡\",\"delayRiskMinutes\":15,\"alternativeMode\":\"택시\","
                    + "\"alternativeReason\":\"퇴근시간대 지하철 혼잡으로 택시가 더 빠릅니다.\"}]}\n"
                    + "[이동 구간 목록]\n" + legPrompt;

            GeminiRequest geminiRequest = new GeminiRequest(List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))));

            GeminiResponse response = callGemini(geminiRequest);

            String aiResult = "";
            if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                aiResult = response.candidates().get(0).content().parts().get(0).text();
            }

            ObjectMapper objectMapper = new ObjectMapper();
            GeminiData recommend = objectMapper.readValue(AiJsonUtil.extractJson(aiResult), GeminiData.class);

            List<TransportLegData> legs = new ArrayList<>();
            if (recommend.getTransportLegs() != null) {
                Map<String, TransportRecommendRequest.TransportStopInput> byTourId = new LinkedHashMap<>();
                for (TransportRecommendRequest.TransportStopInput item : items) {
                    byTourId.putIfAbsent(item.getTourId(), item);
                }

                for (GeminiData.TransportLegItem item : recommend.getTransportLegs()) {
                    TransportRecommendRequest.TransportStopInput from = byTourId.get(item.getFromTourId());
                    TransportRecommendRequest.TransportStopInput to = byTourId.get(item.getToTourId());
                    if (from == null || to == null) continue;

                    TransportLegData leg = new TransportLegData();
                    leg.setDay(item.getDay());
                    leg.setFromTourId(from.getTourId());
                    leg.setFromTourNm(from.getTourNm());
                    leg.setToTourId(to.getTourId());
                    leg.setToTourNm(to.getTourNm());
                    leg.setMode(item.getMode());
                    leg.setDurationMinutes(item.getDurationMinutes());
                    leg.setCost(item.getCost());
                    leg.setTransferCount(item.getTransferCount());
                    leg.setCongestionLevel(item.getCongestionLevel());
                    leg.setDelayRiskMinutes(item.getDelayRiskMinutes());
                    leg.setAlternativeMode(item.getAlternativeMode());
                    leg.setAlternativeReason(item.getAlternativeReason());
                    legs.add(leg);
                }
            }

            return new TransportRecommendResponse(
                    true,
                    200,
                    "SUCCESS",
                    "교통편 추천을 정상적으로 생성했습니다.",
                    "/tourList/transportRecommend",
                    "",
                    legs
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);

            if (AiErrorUtil.isAiOverloaded(e)) {
                return new TransportRecommendResponse(
                        false,
                        503,
                        AiErrorUtil.CODE,
                        AiErrorUtil.MESSAGE,
                        "/tourList/transportRecommend",
                        "",
                        null
                );
            }

            return new TransportRecommendResponse(
                    false,
                    500,
                    "FAIL",
                    "교통편 추천 중 오류가 발생했습니다.",
                    "/tourList/transportRecommend",
                    "",
                    null
            );
        }
    }

    // ========================= 관광지명/개요 번역 =========================
    // 한국관광공사 데이터는 한국어만 제공하므로, 최초 조회 시 Gemini로 번역해
    // TB_TRMA_TOUR_LIST에 캐시해두고 이후에는 캐시된 값을 재사용한다.

    // Gemini 호출 시 주고받은 JSON을 그대로 콘솔에 남긴다. AI 응답이 비거나 이상할 때
    // 이 로그를 보면 실제로 어떤 프롬프트를 보냈고 Gemini가 뭐라고 답했는지 바로 확인할 수 있다.
    private GeminiResponse callGemini(GeminiRequest geminiRequest) {
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println("[Gemini 요청] " + objectMapper.writeValueAsString(geminiRequest));

        // Gemini가 일시적으로 과부하(503)이거나 요청이 몰려 제한(429)에 걸리는 경우가 잦아,
        // 한 번 실패했다고 바로 포기하면 이름/주소 등 번역이 자주 안 되고 한국어 원문만
        // 보이는 문제가 있었다. 과부하성 오류에 한해 짧게 두 번 더 재시도한다.
        RestClientException lastError = null;
        for (int attempt = 1; attempt <= 4; attempt++) {
            try {
                GeminiResponse response = restClient.post()
                        .uri(url)
                        .header("X-goog-api-key", apiKey)
                        .body(geminiRequest)
                        .retrieve()
                        .body(GeminiResponse.class);

                System.out.println("[Gemini 응답] " + objectMapper.writeValueAsString(response));
                return response;
            } catch (RestClientException e) {
                lastError = e;
                if (attempt == 4 || !AiErrorUtil.isAiOverloaded(e)) throw e;
                log.warn("Gemini 호출이 일시적으로 실패해 재시도합니다({}/3).", attempt, e);
                try {
                    Thread.sleep(500L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw lastError;
    }

    private GeminiData callGeminiForTranslation(String prompt) {
        GeminiRequest geminiRequest = new GeminiRequest(List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))));

        GeminiResponse response = callGemini(geminiRequest);

        String aiResult = "";
        if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
            aiResult = response.candidates().get(0).content().parts().get(0).text();
        }
        if (aiResult == null || aiResult.isBlank()) return null;

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(AiJsonUtil.extractJson(aiResult), GeminiData.class);
    }

    private void applySearchTranslations(List<TourSearchData> tours, String lang) {
        if (!"en".equals(lang) && !"ja".equals(lang)) return;
        if (tours == null || tours.isEmpty()) return;

        List<TourSearchData> uncached = new ArrayList<>();
        for (TourSearchData tour : tours) {
            // 한국관광공사 공식 일어/영어 데이터로 이미 채워진 건(NATIVE_MATCH_YN='Y')
            // Gemini로 다시 번역하지 않는다.
            if ("Y".equals(tour.getNativeMatchYn())) continue;

            String cached = "en".equals(lang) ? tour.getTourNmEn() : tour.getTourNmJa();
            if (cached != null && !cached.isBlank()) {
                tour.setTourNm(cached);
            } else {
                uncached.add(tour);
            }
        }
        if (uncached.isEmpty()) return;

        try {
            StringBuilder listPrompt = new StringBuilder();
            for (TourSearchData tour : uncached) {
                listPrompt.append("관광지ID: %s / 이름: %s\n".formatted(tour.getTourId(), tour.getTourNm()));
            }
            String langLabel = "en".equals(lang) ? "영어" : "일본어";
            String prompt = "다음 한국 관광지 이름들을 " + langLabel + "로 자연스럽게 번역해주세요.\n"
                    + "고유명사(궁궐, 지명 등)는 관용적으로 통용되는 표기를 사용하세요.\n"
                    + "반드시 JSON 형식으로만 응답하세요. 다른 설명은 절대 포함하지 마세요.\n"
                    + "[응답 형식]\n"
                    + "{\"translations\":[{\"tourId\":\"T0001\",\"tourNm\":\"번역된 이름\"}]}\n"
                    + "[관광지 목록]\n" + listPrompt;

            GeminiData result = callGeminiForTranslation(prompt);
            if (result == null || result.getTranslations() == null) return;

            Map<String, String> translatedNames = new HashMap<>();
            for (GeminiData.TranslationItem item : result.getTranslations()) {
                if (item.getTourId() != null && item.getTourNm() != null) {
                    translatedNames.put(item.getTourId(), item.getTourNm());
                }
            }

            for (TourSearchData tour : uncached) {
                String translatedNm = translatedNames.get(tour.getTourId());
                if (translatedNm == null || translatedNm.isBlank()) continue;

                tourListMapper.updateTourTranslation(
                        tour.getTourId(),
                        "en".equals(lang) ? translatedNm : null,
                        "ja".equals(lang) ? translatedNm : null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );
                tour.setTourNm(translatedNm);
            }
        } catch (Exception e) {
            log.warn("관광지명 번역에 실패해 한국어로 표시합니다. lang={}", lang, e);
        }
    }

    // 목록 화면(tourSearch)은 이름만 캐시해서 번역하고 주소는 다루지 않아 목록에서
    // 주소가 계속 한국어로 남는 문제가 있었다. 인기 관광지는 여러 사용자의 검색/페이지에
    // 반복해서 등장하므로 이름과 마찬가지로 ROAD_ADDR_EN/JA에 캐시해 재사용한다.
    private void applySearchAddressTranslations(List<TourSearchData> tours, String lang) {
        if (!"en".equals(lang) && !"ja".equals(lang)) return;
        if (tours == null || tours.isEmpty()) return;

        Map<String, String> addrsInput = new LinkedHashMap<>();
        for (TourSearchData tour : tours) {
            // 한국관광공사 공식 데이터로 이미 채워진 주소는 다시 번역하지 않는다.
            // 단, NATIVE_MATCH_YN='Y'라도 공식 데이터에 주소 필드 자체가 비어 있으면
            // COALESCE가 한국어 원문으로 조용히 되돌아가므로, 실제로 한글이 남아있는지
            // 확인해서 그런 경우는 번역 대상에 포함시킨다.
            if ("Y".equals(tour.getNativeMatchYn()) && !AiJsonUtil.containsHangul(tour.getRoadAddr())) continue;

            String cached = "en".equals(lang) ? tour.getRoadAddrEn() : tour.getRoadAddrJa();
            if (cached != null && !cached.isBlank() && !AiJsonUtil.containsHangul(cached)) {
                tour.setRoadAddr(cached);
            } else {
                addrsInput.put(tour.getTourId(), tour.getRoadAddr());
            }
        }
        if (addrsInput.isEmpty()) return;

        Map<String, String> addrs = translateFreeTexts(addrsInput, lang);
        for (TourSearchData tour : tours) {
            String addr = addrs.get(tour.getTourId());
            if (addr == null || addr.isBlank()) continue;

            tourListMapper.updateTourTranslation(
                    tour.getTourId(),
                    null,
                    null,
                    null,
                    null,
                    "en".equals(lang) ? addr : null,
                    "ja".equals(lang) ? addr : null,
                    null,
                    null
            );
            tour.setRoadAddr(addr);
        }
    }

    private void applyDetailTranslation(TourDetailData tour, String lang) {
        if (tour == null) return;
        if (!"en".equals(lang) && !"ja".equals(lang)) return;
        // 한국관광공사 공식 일어/영어 데이터로 이미 채워진 관광지는 Gemini로 다시
        // 번역하지 않는다(tourDetail 쿼리에서 COALESCE로 이미 반영됨).
        // 단, 공식 데이터에 일부 필드(주로 주소)가 비어 있으면 COALESCE가 한국어
        // 원문으로 조용히 되돌아가므로, 실제로 한글이 남아있는 경우는 그대로
        // 믿고 넘어가지 않고 아래 Gemini 폴백까지 진행한다.
        boolean nativeLooksTranslated = "Y".equals(tour.getNativeMatchYn())
                && !AiJsonUtil.containsHangul(tour.getRoadAddr())
                && !AiJsonUtil.containsHangul(tour.getDetailAddr());
        if (nativeLooksTranslated) return;

        String cachedNm = "en".equals(lang) ? tour.getTourNmEn() : tour.getTourNmJa();
        String cachedOverview = "en".equals(lang) ? tour.getOverviewEn() : tour.getOverviewJa();
        String cachedRoadAddr = "en".equals(lang) ? tour.getRoadAddrEn() : tour.getRoadAddrJa();
        String cachedDetailAddr = "en".equals(lang) ? tour.getDetailAddrEn() : tour.getDetailAddrJa();
        // 이름만 캐시된 걸로는 "다 캐시됐다"고 보지 않는다. 목록 화면(tourSearch)은
        // 이름만 먼저 번역해 캐시해두기 때문에, 이름만 보고 판단하면 설명/주소는
        // 영원히 번역을 시도하지 않고 한국어로 남는 문제가 있었다. 설명은 원본이 비어있는
        // 관광지도 있어 판단 기준으로 쓰기 어려우니, 주소(모든 관광지에 항상 있음)까지
        // 같이 캐시돼 있어야 "다 캐시됨"으로 본다.
        boolean fullyCached = cachedNm != null && !cachedNm.isBlank()
                && cachedRoadAddr != null && !cachedRoadAddr.isBlank();
        if (fullyCached) {
            tour.setTourNm(cachedNm);
            if (cachedOverview != null && !cachedOverview.isBlank()) tour.setOverview(cachedOverview);
            tour.setRoadAddr(cachedRoadAddr);
            if (cachedDetailAddr != null && !cachedDetailAddr.isBlank()) tour.setDetailAddr(cachedDetailAddr);
            return;
        }

        try {
            String langLabel = "en".equals(lang) ? "영어" : "일본어";
            String prompt = "다음 한국 관광지의 이름, 설명, 주소를 " + langLabel + "로 자연스럽게 번역해주세요.\n"
                    + "주소는 관용적으로 통용되는 로마자/가나 표기를 사용하고, 우편번호 등 숫자는 그대로 두세요.\n"
                    + "detailAddr가 비어있으면 빈 문자열로 응답하세요.\n"
                    + "반드시 JSON 형식으로만 응답하세요. 다른 설명은 절대 포함하지 마세요.\n"
                    + "[응답 형식]\n"
                    + "{\"translations\":[{\"tourId\":\"" + tour.getTourId() + "\",\"tourNm\":\"번역된 이름\",\"overview\":\"번역된 설명\",\"roadAddr\":\"번역된 도로명주소\",\"detailAddr\":\"번역된 상세주소\"}]}\n"
                    + "[관광지 정보]\n"
                    + "이름: " + tour.getTourNm() + "\n"
                    + "설명: " + (tour.getOverview() == null ? "" : tour.getOverview()) + "\n"
                    + "도로명주소: " + tour.getRoadAddr() + "\n"
                    + "상세주소: " + (tour.getDetailAddr() == null ? "" : tour.getDetailAddr());

            GeminiData result = callGeminiForTranslation(prompt);
            if (result == null || result.getTranslations() == null || result.getTranslations().isEmpty()) return;

            GeminiData.TranslationItem item = result.getTranslations().get(0);
            String tourNm = item.getTourNm();
            String overview = item.getOverview();
            String roadAddr = item.getRoadAddr();
            String detailAddr = item.getDetailAddr();

            tourListMapper.updateTourTranslation(
                    tour.getTourId(),
                    "en".equals(lang) ? tourNm : null,
                    "ja".equals(lang) ? tourNm : null,
                    "en".equals(lang) ? overview : null,
                    "ja".equals(lang) ? overview : null,
                    "en".equals(lang) ? roadAddr : null,
                    "ja".equals(lang) ? roadAddr : null,
                    "en".equals(lang) ? detailAddr : null,
                    "ja".equals(lang) ? detailAddr : null
            );
            if (tourNm != null && !tourNm.isBlank()) tour.setTourNm(tourNm);
            if (overview != null && !overview.isBlank()) tour.setOverview(overview);
            if (roadAddr != null && !roadAddr.isBlank()) tour.setRoadAddr(roadAddr);
            if (detailAddr != null && !detailAddr.isBlank()) tour.setDetailAddr(detailAddr);
        } catch (Exception e) {
            log.warn("관광지 상세 번역에 실패해 한국어로 표시합니다. tourId={}, lang={}", tour.getTourId(), lang, e);
        }
    }

    // ========================= 후기 번역 =========================
    // 관광지명/모임 제목과 동일하게, 최초 조회 시 번역한 결과를 REVIEW_ID 기준으로
    // TB_TRMA_MOIM_REVIEW에 캐시해두고 이후에는 캐시된 값을 재사용한다. 이렇게 하면
    // AI가 끊겨도 이미 한 번 번역된 후기는 계속 정상적으로 보인다.
    private void applyReviewTranslations(List<TourDetailReviewData> reviews, String lang) {
        if (!"en".equals(lang) && !"ja".equals(lang)) return;
        if (reviews == null || reviews.isEmpty()) return;

        List<TourDetailReviewData> uncached = new ArrayList<>();
        for (TourDetailReviewData review : reviews) {
            String cachedTitle = "en".equals(lang) ? review.getReviewTitleEn() : review.getReviewTitleJa();
            String cachedContent = "en".equals(lang) ? review.getReviewContentEn() : review.getReviewContentJa();
            if (cachedContent != null && !cachedContent.isBlank()) {
                if (cachedTitle != null && !cachedTitle.isBlank()) review.setReviewTitle(cachedTitle);
                review.setReviewContent(cachedContent);
            } else {
                uncached.add(review);
            }
        }
        if (uncached.isEmpty()) return;

        try {
            StringBuilder listPrompt = new StringBuilder();
            for (TourDetailReviewData review : uncached) {
                listPrompt.append("[reviewId %s]\n제목: %s\n내용: %s\n\n".formatted(
                        review.getReviewId(),
                        review.getReviewTitle() == null ? "" : review.getReviewTitle(),
                        review.getReviewContent() == null ? "" : review.getReviewContent()
                ));
            }

            String langLabel = "en".equals(lang) ? "영어" : "일본어";
            String prompt = "다음은 여행 후기 목록입니다. 각 후기의 제목과 내용을 " + langLabel + "로 자연스럽게 번역해주세요.\n"
                    + "반드시 JSON 형식으로만 응답하고, 요청받은 reviewId를 그대로 포함해서 응답하세요.\n"
                    + "[응답 형식]\n"
                    + "{\"reviewTranslations\":[{\"reviewId\":\"R0001\",\"reviewTitle\":\"번역된 제목\",\"reviewContent\":\"번역된 내용\"}]}\n"
                    + "[후기 목록]\n" + listPrompt;

            GeminiData result = callGeminiForTranslation(prompt);
            if (result == null || result.getReviewTranslations() == null) return;

            Map<String, GeminiData.ReviewTranslationItem> translated = new HashMap<>();
            for (GeminiData.ReviewTranslationItem item : result.getReviewTranslations()) {
                if (item.getReviewId() != null) translated.put(item.getReviewId(), item);
            }

            for (TourDetailReviewData review : uncached) {
                GeminiData.ReviewTranslationItem item = translated.get(review.getReviewId());
                if (item == null) continue;

                String title = item.getReviewTitle();
                String content = item.getReviewContent();
                if (content == null || content.isBlank()) continue;

                tourListMapper.updateReviewTranslation(
                        review.getReviewId(),
                        "en".equals(lang) && title != null && !title.isBlank() ? title : null,
                        "ja".equals(lang) && title != null && !title.isBlank() ? title : null,
                        "en".equals(lang) ? content : null,
                        "ja".equals(lang) ? content : null
                );
                if (title != null && !title.isBlank()) review.setReviewTitle(title);
                review.setReviewContent(content);
            }
        } catch (Exception e) {
            log.warn("후기 번역에 실패해 한국어로 표시합니다. lang={}", lang, e);
        }
    }
}
