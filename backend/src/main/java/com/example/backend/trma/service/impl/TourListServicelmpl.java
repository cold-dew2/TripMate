package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.*;
import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.mapper.TourListMapper;
import com.example.backend.trma.service.TourListService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TourListServicelmpl implements TourListService {

    private final TourListMapper tourListMapper;
    private final RestClient restClient;

    //사용자 정보 조회
    public TourSearchResponse tourSearch(TourSearchRequest request) {

        try {
            List<TourSearchData> tourSearch = tourListMapper.tourSearch(request);
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

            e.printStackTrace();

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

            prompt = prompt + "[요청 사항]\n" +
                    "1. 해당 관광지의 운영시간, 휴무일, 입장료, 공식/관련 홈페이지 URL, 주차 정보(가능 여부 및 요금)를 정확하게 작성해줘.\n" +
                    "2. 정보가 불확실하거나 수집할 수 없는 항목은 null로 표시해줘.\n" +
                    "3. 부연 설명이나 인삿말은 모두 제외하고, 오직 순수한 JSON 데이터만 반환해줘.\n" +
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
            List<TourDetailReviewData> tourDetailReview = tourListMapper.tourDetailReview(request.getTourId());

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
}
