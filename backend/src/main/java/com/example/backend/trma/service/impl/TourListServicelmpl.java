package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.*;
import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.mapper.TourListMapper;
import com.example.backend.trma.service.TourListService;
import com.example.backend.trma.util.AiErrorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
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
            if(request.getKeyword() == null || request.getKeyword().equals("")){
                request2.setKeyword("여름");
            }else{
                request2.setKeyword(request.getKeyword());
            }
            request2.setCateCd(request.getCateCd());

            List<TourSearchData> tourSearch = tourListMapper.tourSearch(request2);
            if (tourSearch == null || tourSearch.isEmpty()) {
                // 테스트
                prompt = prompt + "[관광지 목록]\n" +
                        "[관광지 번호 001]\n" +
                        "관광지ID : tour001\n" +
                        "관광지명 : 경복궁\n" +
                        "도로명주소 : 서울특별시 종로구 사직로 161\n" +
                        "카테고리 : 역사, 한복체험\n\n" +

                        "[관광지 번호 002]\n" +
                        "관광지ID : tour002\n" +
                        "관광지명 : 남이섬\n" +
                        "도로명주소 : 강원특별자치도 춘천시 남산면 남이섬길 1\n" +
                        "카테고리 : 자연, 산책\n\n" +

                        "[관광지 번호 003]\n" +
                        "관광지ID : tour003\n" +
                        "관광지명 : 설악산국립공원\n" +
                        "도로명주소 : 강원특별자치도 속초시 설악산로 1091\n" +
                        "카테고리 : 자연, 등산\n\n" +

                        "[관광지 번호 004]\n" +
                        "관광지ID : tour004\n" +
                        "관광지명 : 아쿠아플라넷 여수\n" +
                        "도로명주소 : 전라남도 여수시 오동도로 61-11\n" +
                        "카테고리 : 가족, 실내\n\n" +

                        "[관광지 번호 005]\n" +
                        "관광지ID : tour005\n" +
                        "관광지명 : 전주한옥마을\n" +
                        "도로명주소 : 전북특별자치도 전주시 완산구 기린대로 99\n" +
                        "카테고리 : 한옥, 전통문화\n\n" +

                        "[관광지 번호 006]\n" +
                        "관광지ID : tour006\n" +
                        "관광지명 : 해운대해수욕장\n" +
                        "도로명주소 : 부산광역시 해운대구 해운대해변로 264\n" +
                        "카테고리 : 바다, 해수욕\n\n" +

                        "[관광지 번호 007]\n" +
                        "관광지ID : tour007\n" +
                        "관광지명 : 제주 성산일출봉\n" +
                        "도로명주소 : 제주특별자치도 서귀포시 성산읍 일출로 284-12\n" +
                        "카테고리 : 자연, 일출\n\n" +

                        "[관광지 번호 008]\n" +
                        "관광지ID : tour008\n" +
                        "관광지명 : 안동 하회마을\n" +
                        "도로명주소 : 경상북도 안동시 풍천면 전서로 186\n" +
                        "카테고리 : 전통마을, 문화유산\n\n" +

                        "[관광지 번호 009]\n" +
                        "관광지ID : tour009\n" +
                        "관광지명 : 에버랜드\n" +
                        "도로명주소 : 경기도 용인시 처인구 포곡읍 에버랜드로 199\n" +
                        "카테고리 : 테마파크, 가족\n\n" +

                        "[관광지 번호 010]\n" +
                        "관광지ID : tour010\n" +
                        "관광지명 : 순천만국가정원\n" +
                        "도로명주소 : 전라남도 순천시 국가정원1호길 47\n" +
                        "카테고리 : 정원, 자연\n";

            } else {
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
            }

                prompt = prompt + "응답 예시\n" +
                        "{\n" +
                        "  \"recommendations\":[\n" +
                        "    {\n" +
                        "      \"tourId\":\"T0001\",\n" +
                        "      \"score\":\"4.8\"" +
                        "      \"reason\":\"자연경관이 뛰어나며 가족과 함께 산책하기 좋습니다.\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"tourId\":\"T0005\",\n" +
                        "      \"score\":\"4.3\"" +
                        "      \"reason\":\"아이들과 체험하기 좋은 실내 관광지입니다.\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"tourId\":\"T0012\",\n" +
                        "      \"score\":\"3.7\"" +
                        "      \"reason\":\"야경이 아름답고 커플 여행에 적합합니다.\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

            GeminiRequest geminiRequest = new GeminiRequest(List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))));

            GeminiResponse response = restClient.post()
                    .uri(url)
                    .header("X-goog-api-key", apiKey)
                    .body(geminiRequest)
                    .retrieve()
                    .body(GeminiResponse.class);

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
            TourDetailData tourDetail = tourListMapper.tourDetail(request.getTourId());
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

        try {
            StringBuilder tourListPrompt = new StringBuilder();
            TourDetailData tourDetail = tourListMapper.tourDetail(request.getTourId());

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

            GeminiResponse response = restClient.post()
                    .uri(url)
                    .header("X-goog-api-key", apiKey)
                    .body(geminiRequest)
                    .retrieve()
                    .body(GeminiResponse.class);

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
                    objectMapper.readValue(aiResult, GeminiData.Recommendation3.class);

            TourAiDetailData tourAiDetail = new TourAiDetailData();

            tourAiDetail.setOperatingHours(recommend.getOperatingHours());
            tourAiDetail.setClosedDays(recommend.getClosedDays());
            tourAiDetail.setAdmissionFeeIsFree(recommend.getAdmissionFeeIsFree());
            tourAiDetail.setAdmissionFeeDetails(recommend.getAdmissionFeeDetails());
            tourAiDetail.setWebsiteUrl(recommend.getWebsiteUrl());
            tourAiDetail.setParkingAvailable(recommend.getParkingAvailable());
            tourAiDetail.setParkingFeeInfo(recommend.getParkingFeeInfo());
            tourAiDetail.setLastUpdatedNote(recommend.getLastUpdatedNote());

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
            if (tourList == null || tourList.isEmpty()) {
                searchRequest.setKeyword("여행");
                tourList = tourListMapper.tourSearch(searchRequest);
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

            String prompt = "당신은 대한민국 여행 일정 플래너입니다.\n"
                    + "다음 관광지 목록 중에서만 골라, 아래 조건에 맞는 " + dayCount + "일 여행 일정을 만들어주세요.\n"
                    + "[여행 조건]\n" + conditionPrompt
                    + "[규칙]\n"
                    + "1. 반드시 제공된 목록에 있는 관광지ID만 사용하세요.\n"
                    + "2. 하루에 2~3개의 관광지를 배정하세요.\n"
                    + "3. 시간은 09:00~18:00 사이로, 이동 시간을 고려해 배정하세요.\n"
                    + "4. 하루 안에서는 시간 순서대로 정렬하세요.\n"
                    + "5. 관심 테마/여행 목적과 동행 인원을 고려해 어울리는 관광지 위주로 배정하세요.\n"
                    + "6. 반드시 JSON 형식으로만 응답하세요. 다른 설명은 절대 포함하지 마세요.\n"
                    + "[응답 형식]\n"
                    + "{\"recommendations4\":[{\"day\":1,\"time\":\"10:00\",\"tourId\":\"T0001\"}]}\n"
                    + "[관광지 목록]\n" + tourListPrompt;

            GeminiRequest geminiRequest = new GeminiRequest(List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))));

            GeminiResponse response = restClient.post()
                    .uri(url)
                    .header("X-goog-api-key", apiKey)
                    .body(geminiRequest)
                    .retrieve()
                    .body(GeminiResponse.class);

            String aiResult = "";
            if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                aiResult = response.candidates().get(0).content().parts().get(0).text();
            }

            ObjectMapper objectMapper = new ObjectMapper();
            GeminiData recommend = objectMapper.readValue(aiResult, GeminiData.class);

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
                    + "[응답 형식]\n"
                    + "{\"transportLegs\":[{\"day\":1,\"fromTourId\":\"T0001\",\"toTourId\":\"T0002\","
                    + "\"mode\":\"지하철\",\"durationMinutes\":20,\"cost\":1500,\"transferCount\":0,"
                    + "\"congestionLevel\":\"혼잡\",\"delayRiskMinutes\":15,\"alternativeMode\":\"택시\","
                    + "\"alternativeReason\":\"퇴근시간대 지하철 혼잡으로 택시가 더 빠릅니다.\"}]}\n"
                    + "[이동 구간 목록]\n" + legPrompt;

            GeminiRequest geminiRequest = new GeminiRequest(List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))));

            GeminiResponse response = restClient.post()
                    .uri(url)
                    .header("X-goog-api-key", apiKey)
                    .body(geminiRequest)
                    .retrieve()
                    .body(GeminiResponse.class);

            String aiResult = "";
            if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                aiResult = response.candidates().get(0).content().parts().get(0).text();
            }

            ObjectMapper objectMapper = new ObjectMapper();
            GeminiData recommend = objectMapper.readValue(aiResult, GeminiData.class);

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

    private GeminiData callGeminiForTranslation(String prompt) {
        GeminiRequest geminiRequest = new GeminiRequest(List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))));

        GeminiResponse response = restClient.post()
                .uri(url)
                .header("X-goog-api-key", apiKey)
                .body(geminiRequest)
                .retrieve()
                .body(GeminiResponse.class);

        String aiResult = "";
        if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
            aiResult = response.candidates().get(0).content().parts().get(0).text();
        }
        if (aiResult == null || aiResult.isBlank()) return null;

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(aiResult, GeminiData.class);
    }

    private void applySearchTranslations(List<TourSearchData> tours, String lang) {
        if (!"en".equals(lang) && !"ja".equals(lang)) return;
        if (tours == null || tours.isEmpty()) return;

        List<TourSearchData> uncached = new ArrayList<>();
        for (TourSearchData tour : tours) {
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

    private void applyDetailTranslation(TourDetailData tour, String lang) {
        if (tour == null) return;
        if (!"en".equals(lang) && !"ja".equals(lang)) return;

        String cachedNm = "en".equals(lang) ? tour.getTourNmEn() : tour.getTourNmJa();
        String cachedOverview = "en".equals(lang) ? tour.getOverviewEn() : tour.getOverviewJa();
        String cachedRoadAddr = "en".equals(lang) ? tour.getRoadAddrEn() : tour.getRoadAddrJa();
        String cachedDetailAddr = "en".equals(lang) ? tour.getDetailAddrEn() : tour.getDetailAddrJa();
        if (cachedNm != null && !cachedNm.isBlank()) {
            tour.setTourNm(cachedNm);
            if (cachedOverview != null && !cachedOverview.isBlank()) tour.setOverview(cachedOverview);
            if (cachedRoadAddr != null && !cachedRoadAddr.isBlank()) tour.setRoadAddr(cachedRoadAddr);
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
    // 후기는 사용자가 계속 새로 작성하는 데이터라 이름/개요처럼 DB에 캐시하지 않고,
    // 조회 시점에 해당 페이지(최대 10건)만 매번 번역한다. 후기에는 안정적인 ID가 없으므로
    // 요청 시 배열 순서(index)로 보내고 그대로 매칭해서 되돌려 받는다.
    private void applyReviewTranslations(List<TourDetailReviewData> reviews, String lang) {
        if (!"en".equals(lang) && !"ja".equals(lang)) return;
        if (reviews == null || reviews.isEmpty()) return;

        try {
            StringBuilder listPrompt = new StringBuilder();
            for (int i = 0; i < reviews.size(); i++) {
                TourDetailReviewData review = reviews.get(i);
                listPrompt.append("[index %d]\n제목: %s\n내용: %s\n\n".formatted(
                        i,
                        review.getReviewTitle() == null ? "" : review.getReviewTitle(),
                        review.getReviewContent() == null ? "" : review.getReviewContent()
                ));
            }

            String langLabel = "en".equals(lang) ? "영어" : "일본어";
            String prompt = "다음은 여행 후기 목록입니다. 각 후기의 제목과 내용을 " + langLabel + "로 자연스럽게 번역해주세요.\n"
                    + "반드시 JSON 형식으로만 응답하고, 요청받은 index를 그대로 포함해서 응답하세요.\n"
                    + "[응답 형식]\n"
                    + "{\"reviewTranslations\":[{\"index\":0,\"reviewTitle\":\"번역된 제목\",\"reviewContent\":\"번역된 내용\"}]}\n"
                    + "[후기 목록]\n" + listPrompt;

            GeminiData result = callGeminiForTranslation(prompt);
            if (result == null || result.getReviewTranslations() == null) return;

            for (GeminiData.ReviewTranslationItem item : result.getReviewTranslations()) {
                if (item.getIndex() < 0 || item.getIndex() >= reviews.size()) continue;

                TourDetailReviewData review = reviews.get(item.getIndex());
                if (item.getReviewTitle() != null && !item.getReviewTitle().isBlank()) {
                    review.setReviewTitle(item.getReviewTitle());
                }
                if (item.getReviewContent() != null && !item.getReviewContent().isBlank()) {
                    review.setReviewContent(item.getReviewContent());
                }
            }
        } catch (Exception e) {
            log.warn("후기 번역에 실패해 한국어로 표시합니다. lang={}", lang, e);
        }
    }
}
