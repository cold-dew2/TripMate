package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.*;
import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.GeminiResponse;
import com.example.backend.trma.dto.response.MoimAiSearchResponse;
import com.example.backend.trma.dto.response.MoimDetailResponse;
import com.example.backend.trma.dto.response.MoimSearchResponse;
import com.example.backend.trma.mapper.MoimListMapper;
import com.example.backend.trma.service.MoimListService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MoimListServicelmpl implements MoimListService {

    private final MoimListMapper moimListMapper;
    private final RestClient restClient;

    //사용자 정보 조회
    public MoimSearchResponse moimSearch(MoimSearchRequest request) {

        try {
            List<MoimSearchData> moimSearch = moimListMapper.moimSearch(request);
            return new MoimSearchResponse(
                    true,
                    200,
                    "SUCCESS",
                    "소모임 정보를 정상적으로 조회했습니다.",
                    "/moimList/moimSearch",
                    "",
                    moimSearch
            );
        } catch (Exception e) {
            return new MoimSearchResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/moimList/moimSearch",
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
    public MoimAiSearchResponse moimAiSearch(MoimAiSearchRequest request) {
        try {
            String keyword = request.getKeyword();
            String cateCd = request.getCateCd();

            String prompt = ""
                    + "당신은 여행 커뮤니티 서비스 \"TripMate\"의 AI 추천 전문가입니다.\n"
                    + "다음 규칙을 반드시 지켜주세요.\n"
                    + "\n"
                    + "[역할]\n"
                    + "현재 모집 중인 소모임 목록을 분석하여 일반 사용자가 참여하기 좋은 소모임을 추천합니다.\n"
                    + "\n"
                    + "[추천 기준]\n"
                    + "1. 모집 중인 소모임만 추천하세요.\n"
                    + "2. 모집이 마감된 소모임은 추천하지 마세요.\n"
                    + "3. 시작일이 지난 소모임은 추천하지 마세요.\n"
                    + "4. 방문수(VISIT_CNT)가 많은 소모임에 가점을 부여하세요.\n"
                    + "5. 현재 참여 인원과 최대 인원을 고려하여 모집률이 적절한 소모임을 우선 추천하세요.\n"
                    + "6. 모임 설명(MOIM_DSCR)이 구체적이고 매력적인 소모임을 우선 추천하세요.\n"
                    + "7. 동일한 카테고리만 추천하지 말고 다양한 카테고리를 포함하세요.\n"
                    + "8. 추천 점수는 0~100점으로 계산하세요.\n"
                    + "9. 80점 이상인 소모임만 추천하세요.\n"
                    + "10. 최대 5개의 소모임만 추천하세요.\n"
                    + "\n"
                    + "[출력 규칙]\n"
                    + "1. 반드시 제공된 소모임 목록 안에서만 추천하세요.\n"
                    + "2. 목록에 없는 소모임은 절대로 추천하지 마세요.\n"
                    + "3. 추천 이유는 100자 이내로 작성하세요.\n"
                    + "4. 추천 점수가 높은 순으로 정렬하세요.\n"
                    + "5. 반드시 JSON 형식으로만 응답하세요.\n"
                    + "6. JSON 이외의 설명이나 문장은 작성하지 마세요.\n"
                    + "\n"
                    + "[응답 형식]\n"
                    + "{\n"
                    + "  \"recommendations\": [\n"
                    + "    {\n"
                    + "      \"moimId\": \"TRMAMOIM00001\",\n"
                    + "      \"score\": 95,\n"
                    + "      \"reason\": \"자연과 힐링을 즐길 수 있으며 모집률과 방문수가 높아 참여 가치가 높습니다.\"\n"
                    + "    }\n"
                    + "  ]\n"
                    + "}\n";

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

            MoimSearchRequest request2 = new MoimSearchRequest();
            if(request.getKeyword() == null || request.getKeyword().equals("")){
                request2.setKeyword("여름");
            }else{
                request2.setKeyword(request.getKeyword());
            }
            request2.setCateCd(request.getCateCd());

            List<MoimSearchData> moimSearch = moimListMapper.moimSearch(request2);
            if (moimSearch == null || moimSearch.isEmpty()) {
                // 테스트
                prompt = prompt + "[현재 모집 중인 소모임]\n" +
                        "[소모임 번호 001]\n" +
                        "소모임ID : TRMAMOIM00001\n" +
                        "소모임명 : 강원도 힐링 캠핑\n" +
                        "소모임설명 : 강원도 자연 속에서 1박 2일 캠핑과 바비큐를 함께 즐기는 힐링 모임입니다.\n" +
                        "카테고리 : 캠핑, 자연, 힐링\n\n" +

                        "[소모임 번호 002]\n" +
                        "소모임ID : TRMAMOIM00002\n" +
                        "소모임명 : 서울 감성 카페 투어\n" +
                        "소모임설명 : 서울의 유명 감성 카페를 함께 방문하며 사진도 찍고 이야기를 나누는 모임입니다.\n" +
                        "카테고리 : 카페, 사진명소\n\n" +

                        "[소모임 번호 003]\n" +
                        "소모임ID : TRMAMOIM00003\n" +
                        "소모임명 : 부산 바다 드라이브\n" +
                        "소모임설명 : 해안도로를 따라 드라이브를 즐기고 바다 풍경을 감상하는 여행 모임입니다.\n" +
                        "카테고리 : 바다, 드라이브\n\n" +

                        "[소모임 번호 004]\n" +
                        "소모임ID : TRMAMOIM00004\n" +
                        "소모임명 : 전주 맛집 탐방\n" +
                        "소모임설명 : 전주 한옥마을과 다양한 현지 맛집을 함께 즐기는 미식 여행입니다.\n" +
                        "카테고리 : 맛집, 문화\n\n" +

                        "[소모임 번호 005]\n" +
                        "소모임ID : TRMAMOIM00005\n" +
                        "소모임명 : 제주 오름 트레킹\n" +
                        "소모임설명 : 제주 오름을 걸으며 자연을 만끽하고 인생 사진을 남기는 모임입니다.\n" +
                        "카테고리 : 자연, 산, 사진명소\n\n" +

                        "[소모임 번호 006]\n" +
                        "소모임ID : TRMAMOIM00006\n" +
                        "소모임명 : 경주 역사 여행\n" +
                        "소모임설명 : 신라의 역사 유적을 둘러보고 문화 해설과 함께 여행하는 모임입니다.\n" +
                        "카테고리 : 역사, 문화\n\n" +

                        "[소모임 번호 007]\n" +
                        "소모임ID : TRMAMOIM00007\n" +
                        "소모임명 : 가족과 함께 공원 나들이\n" +
                        "소모임설명 : 아이들과 함께 공원에서 피크닉과 다양한 체험을 즐기는 가족 모임입니다.\n" +
                        "카테고리 : 가족여행, 공원, 아이와 함께\n\n" +

                        "[소모임 번호 008]\n" +
                        "소모임ID : TRMAMOIM00008\n" +
                        "소모임명 : 야경 출사 모임\n" +
                        "소모임설명 : 서울의 아름다운 야경 명소를 방문하여 사진 촬영을 함께하는 모임입니다.\n" +
                        "카테고리 : 야경, 사진명소\n\n" +

                        "[소모임 번호 009]\n" +
                        "소모임ID : TRMAMOIM00009\n" +
                        "소모임명 : 반려견과 떠나는 여행\n" +
                        "소모임설명 : 반려동물과 함께 여행할 수 있는 명소를 방문하는 소규모 여행 모임입니다.\n" +
                        "카테고리 : 반려동물 동반, 산책\n\n" +

                        "[소모임 번호 010]\n" +
                        "소모임ID : TRMAMOIM00010\n" +
                        "소모임명 : 온천 힐링 여행\n" +
                        "소모임설명 : 온천에서 휴식을 취하고 맛집까지 함께 즐기는 힐링 여행 모임입니다.\n" +
                        "카테고리 : 온천·스파, 힐링, 맛집\n\n";

            } else {
                StringBuilder moimListPrompt = new StringBuilder();

                prompt = prompt + "[관광지 목록]\n";

                int no = 1;

                for (MoimSearchData moim : moimSearch) {
                    moimListPrompt.append("""
                            [소모임 번호 %03d]
                            소모임ID : %s
                            소모임명 : %s
                            소모임설명 : %s
                            카테고리 : %s
                            """.formatted(
                            no++,
                            moim.getMoimId(),
                            moim.getMoimTitle(),
                            moim.getMoimDscr(),
                            moim.getCateNm()
                    ));
                }

                prompt = prompt + moimListPrompt;
            }

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

            List<MoimAiSearchData> moimAiSearchData = new ArrayList<>();

            for (GeminiData.Recommendation2 item : recommend.getRecommendations2()) {

                MoimSearchData moimInfo = moimListMapper.moimInfo(item.getMoimId());

                MoimAiSearchData data = getMoimAiSearchData(item, moimInfo);

                moimAiSearchData.add(data);
            }

            return new MoimAiSearchResponse(
                    true,
                    200,
                    "SUCCESS",
                    "AI 소모임 추천 정보를 정상적으로 조회했습니다.",
                    "/moimList/moimAiSearch",
                    "",
                    moimAiSearchData
            );

        } catch (Exception e) {

            e.printStackTrace();

            return new MoimAiSearchResponse(
                    false,
                    500,
                    "FAIL",
                    e.getMessage(),
                    "/moimList/moimAiSearch",
                    "",
                    null
            );
        }
    }

    private static @NonNull MoimAiSearchData getMoimAiSearchData(GeminiData.Recommendation2 item, MoimSearchData moimInfo) {
        MoimAiSearchData data = new MoimAiSearchData();

        data.setMoimId(item.getMoimId());
        if (moimInfo != null) {
            data.setMoimTitle(moimInfo.getMoimTitle());
            data.setMoimDscr(moimInfo.getMoimDscr());
            data.setMoimStartDt(moimInfo.getMoimEndDt());
            data.setUserNm(moimInfo.getUserNm());
            data.setVisitCnt(moimInfo.getVisitCnt());
            data.setMaxMember(moimInfo.getMaxMember());
            data.setMemberCnt(moimInfo.getMemberCnt());
            data.setCateCd(moimInfo.getCateCd());
            data.setCateNm(moimInfo.getCateNm());
        }
        data.setScore(item.getScore());
        data.setReason(item.getReason());
        return data;
    }

    //모임 상세조회(기본)
    public MoimDetailResponse moimDetail(MoimDetailRequest request) {

        try {
            MoimDetailData moimDetail = moimListMapper.moimDetail(request.getMoimId());
            List<MoimCateData> moimCate = moimListMapper.moimCate(request.getMoimId());
            List<MoimPlanData> moimPlan = moimListMapper.moimPlan(request.getMoimId());
            return new MoimDetailResponse(
                    true,
                    200,
                    "SUCCESS",
                    "모임 상세조회(기본)을 정상적으로 조회했습니다.",
                    "/moimList/moimDetail",
                    "",
                    moimDetail,
                    moimCate,
                    moimPlan
            );
        } catch (Exception e) {
            return new MoimDetailResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/moimList/moimDetail",
                    "",
                    null,
                    null,
                    null
            );
        }
    }
}
