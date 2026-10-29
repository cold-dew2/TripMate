package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.*;
import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.mapper.ChatMapper;
import com.example.backend.trma.mapper.MoimListMapper;
import com.example.backend.trma.mapper.NotificationMapper;
import com.example.backend.trma.service.MoimListService;
import com.example.backend.trma.service.NotificationPushService;
import com.example.backend.trma.service.TourListService;
import com.example.backend.trma.util.AiErrorUtil;
import com.example.backend.trma.util.AiJsonUtil;
import com.example.backend.trma.util.ContentTranslationHelper;
import com.example.backend.trma.util.ContentLangUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoimListServicelmpl implements MoimListService {

    private final MoimListMapper moimListMapper;
    private final ChatMapper chatMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationPushService notificationPushService;
    private final RestClient restClient;
    private final TourListService tourListService;

    //사용자 정보 조회
    public MoimSearchResponse moimSearch(MoimSearchRequest request) {

        try {
            if(request.getPage() == 0){
                request.setPage(1);
            }
            int offset = (request.getPage() - 1) * 10 ;
            request.setOffset(offset);

            List<MoimSearchData> moimSearch = moimListMapper.moimSearch(request);
            // 다른 목록 화면(tourSearch 등)과 동일하게, 번역 대기를 0.5초로 제한해
            // 캐시가 없는 첫 조회에서도 응답이 오래 붙잡히지 않게 한다.
            try {
                CompletableFuture.runAsync(() -> applyMoimSearchTranslations(moimSearch, request.getLang()))
                        .get(500, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                log.warn("소모임 목록 번역이 0.5초 안에 끝나지 않아 한국어로 먼저 응답합니다. lang={}", request.getLang());
            } catch (Exception e) {
                log.warn("소모임 목록 번역 대기 중 오류가 발생했습니다. lang={}", request.getLang(), e);
            }
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
            log.error("처리 중 오류가 발생했습니다.", e);
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
            request2.setKeyword(request.getKeyword());
            request2.setCateCd(request.getCateCd());

            List<MoimSearchData> moimSearch = moimListMapper.moimSearch(request2);

            // 검색어+테마 조합에 맞는 모집 중인 소모임이 없으면 테마만으로, 그래도 없으면
            // 테마도 빼고 다시 찾는다. 예전에는 여기서 DB에 존재하지 않는 가짜 소모임
            // 목록을 프롬프트에 넣었는데, Gemini가 그 가짜 ID(TRMAMOIM00001 등, 실제로는
            // 다른 소모임에 쓰이고 있을 수도 있는 ID)를 추천하면 이후 실제 조회 결과와
            // 어긋나는 문제가 있었다. 항상 실제 DB에 있는 소모임만 후보로 준다.
            if ((moimSearch == null || moimSearch.isEmpty())
                    && request2.getKeyword() != null && !request2.getKeyword().isBlank()) {
                MoimSearchRequest cateOnly = new MoimSearchRequest();
                cateOnly.setCateCd(request.getCateCd());
                moimSearch = moimListMapper.moimSearch(cateOnly);
            }
            if (moimSearch == null || moimSearch.isEmpty()) {
                moimSearch = moimListMapper.moimSearch(new MoimSearchRequest());
            }

            StringBuilder moimListPrompt = new StringBuilder();

            prompt = prompt + "[현재 모집 중인 소모임]\n";

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
                    objectMapper.readValue(AiJsonUtil.extractJson(aiResult), GeminiData.class);

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
            log.error("처리 중 오류가 발생했습니다.", e);

            if (AiErrorUtil.isAiOverloaded(e)) {
                return new MoimAiSearchResponse(
                        false,
                        503,
                        AiErrorUtil.CODE,
                        AiErrorUtil.MESSAGE,
                        "/moimList/moimAiSearch",
                        "",
                        null
                );
            }

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
    public MoimDetailResponse moimDetail(MoimDetailRequest request, String userId) {

        try {
            MoimDetailData moimDetail = moimListMapper.moimDetail(request.getMoimId());
            if (moimDetail == null) {
                return new MoimDetailResponse(
                        false,
                        404,
                        "MOIM_DELETED",
                        "삭제되었거나 존재하지 않는 모임입니다.",
                        "/moimList/moimDetail",
                        "",
                        null, null, null, null, null
                );
            }
            List<MoimCateData> moimCate = moimListMapper.moimCate(request.getMoimId());
            List<MoimPlanData> moimPlan = moimListMapper.moimPlan(request.getMoimId(), request.getLang());

            // 제목/소개 번역과 일정 관광지명 번역은 서로 무관하니, 캐시가 없어 Gemini를
            // 둘 다 호출해야 하는 최초 조회(cold-cache)에서 순차 대기로 응답이 느려지지
            // 않도록 동시에 실행한다(리뷰 작성 화면처럼 이 정보만 필요한 "고정 페이지"도
            // moimDetail을 그대로 쓰기 때문에 특히 체감이 컸다).
            CompletableFuture<Void> titleTranslation = CompletableFuture.runAsync(
                    () -> applyMoimDetailTranslation(moimDetail, request.getLang()));
            CompletableFuture<Void> planTranslation = CompletableFuture.runAsync(
                    () -> applyMoimPlanTranslation(moimPlan, request.getLang()));
            // 캐시가 완전히 비어 있는 최초 조회는 Gemini 응답이 늦어지면 3초를 넘기기
            // 쉽다. 응답 시간을 보장하기 위해 번역 완료를 최대 0.5초만 기다리고, 그 안에
            // 못 끝나면 아직 한국어인 상태로라도 바로 응답한다 — 두 작업은 취소하지
            // 않고 계속 돌게 둬서, 이번 응답에는 못 실려도 캐시(DB)에는 저장되어 다음
            // 조회부터는 번역된 채로 즉시 나온다.
            try {
                CompletableFuture.allOf(titleTranslation, planTranslation).get(500, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                log.warn("모임 상세 번역이 0.5초 안에 끝나지 않아 한국어로 먼저 응답합니다. moimId={}, lang={}",
                        request.getMoimId(), request.getLang());
            } catch (Exception e) {
                log.warn("모임 상세 번역 대기 중 오류가 발생했습니다. moimId={}, lang={}", request.getMoimId(), request.getLang(), e);
            }
            MoimJoinStatusData moimJoinStatus = moimListMapper.moimJoinStatus(request.getMoimId(), userId);
            MoimReviewStatusData moimReviewStatus = moimListMapper.moimReviewStatus(request.getMoimId(), userId);

            // 조회수 집계용 방문 이력. 비로그인 게스트는 사용자별로 구분할 수 없어 남기지
            // 않고, 이 기록이 실패해도 상세조회 자체는 이미 완료된 것으로 처리해야 하므로
            // 별도로 감싸서 조회 성공 여부에 영향을 주지 않게 한다.
            if (userId != null) {
                try {
                    moimListMapper.insertMoimVisitIfNotExists(request.getMoimId(), userId);
                } catch (Exception e) {
                    log.warn("모임 방문 이력 등록에 실패했습니다. moimId={}", request.getMoimId(), e);
                }
            }

            return new MoimDetailResponse(
                    true,
                    200,
                    "SUCCESS",
                    "모임 상세조회(기본)을 정상적으로 조회했습니다.",
                    "/moimList/moimDetail",
                    "",
                    moimDetail,
                    moimCate,
                    moimPlan,
                    moimJoinStatus,
                    moimReviewStatus
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new MoimDetailResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/moimList/moimDetail",
                    "",
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    //모임 테마 조회
    public MoimCateSearchResponse moimCateSearch() {

        try {
            List<MoimCateData> moimCateList = moimListMapper.moimCateSearch();

            return new MoimCateSearchResponse(
                    true,
                    200,
                    "SUCCESS",
                    "모임 테마 조회를 정상적으로 조회했습니다.",
                    "/moimList/moimDetail",
                    "",
                    moimCateList
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new MoimCateSearchResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/moimList/moimDetail",
                    "",
                    null
            );
        }
    }

    //내 모임 목록 조회
    public MyMoimResponse myMoim(String userId, String lang) {

        try {
            List<MyMoimData> myMoimList = moimListMapper.myMoim(userId);
            if (ContentLangUtil.isSupported(lang)) {
                List<String> moimIds = myMoimList.stream().map(MyMoimData::getMoimId).toList();
                // 다른 목록과 동일하게 번역 대기를 0.5초로 제한한다.
                try {
                    CompletableFuture.supplyAsync(() -> translateMoimTitles(moimIds, lang))
                            .thenAccept(titles -> {
                                for (MyMoimData moim : myMoimList) {
                                    String title = titles.get(moim.getMoimId());
                                    if (title != null && !title.isBlank()) moim.setMoimTitle(title);
                                }
                            })
                            .get(500, java.util.concurrent.TimeUnit.MILLISECONDS);
                } catch (java.util.concurrent.TimeoutException e) {
                    log.warn("내 모임 목록 번역이 0.5초 안에 끝나지 않아 한국어로 먼저 응답합니다. lang={}", lang);
                } catch (Exception e) {
                    log.warn("내 모임 목록 번역 대기 중 오류가 발생했습니다. lang={}", lang, e);
                }
            }

            return new MyMoimResponse(
                    true,
                    200,
                    "SUCCESS",
                    "내 모임 목록 조회를 정상적으로 조회했습니다.",
                    "/moimList/myMoim",
                    "",
                    myMoimList
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new MyMoimResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/moimList/myMoim",
                    "",
                    null
            );
        }
    }

    //내가 가입한 모임 중 오늘 진행 중인 모임들의 오늘 일정
    @Override
    public MyTodayScheduleResponse myTodaySchedule(String userId, String lang) {

        try {
            String today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).toString();
            List<MyTodayScheduleRowData> rows = moimListMapper.myTodaySchedule(userId, today);

            Map<String, String> translatedTitles = Map.of();
            Map<String, String> translatedPlaceNames = Map.of();
            if (ContentLangUtil.isSupported(lang)) {
                // 모임 제목 번역과 장소명 번역은 서로 무관하니 동시에 실행하고, 0.5초로
                // 대기를 제한한다(다른 목록 화면과 동일한 이유).
                CompletableFuture<Map<String, String>> titleFuture = CompletableFuture.supplyAsync(
                        () -> translateMoimTitles(rows.stream().map(MyTodayScheduleRowData::getMoimId).distinct().toList(), lang));
                CompletableFuture<Map<String, String>> placeFuture = CompletableFuture.supplyAsync(
                        () -> tourListService.translateTourNames(
                                rows.stream().map(MyTodayScheduleRowData::getTourId).filter(java.util.Objects::nonNull).distinct().toList(), lang));
                try {
                    CompletableFuture.allOf(titleFuture, placeFuture).get(500, java.util.concurrent.TimeUnit.MILLISECONDS);
                    translatedTitles = titleFuture.getNow(Map.of());
                    translatedPlaceNames = placeFuture.getNow(Map.of());
                } catch (java.util.concurrent.TimeoutException e) {
                    log.warn("오늘 일정 번역이 0.5초 안에 끝나지 않아 한국어로 먼저 응답합니다. lang={}", lang);
                } catch (Exception e) {
                    log.warn("오늘 일정 번역 대기 중 오류가 발생했습니다. lang={}", lang, e);
                }
            }

            // MOIM_ID 기준으로 묶는다. 쿼리가 이미 MOIM_ID로 정렬돼 있어 LinkedHashMap으로
            // 순서를 그대로 유지한다.
            java.util.LinkedHashMap<String, String> titleByMoimId = new java.util.LinkedHashMap<>();
            java.util.Map<String, List<MyTodayScheduleItemData>> itemsByMoimId = new java.util.LinkedHashMap<>();
            for (MyTodayScheduleRowData row : rows) {
                String title = translatedTitles.getOrDefault(row.getMoimId(), row.getMoimTitle());
                titleByMoimId.putIfAbsent(row.getMoimId(), title);
                List<MyTodayScheduleItemData> items = itemsByMoimId.computeIfAbsent(row.getMoimId(), k -> new ArrayList<>());
                if (row.getTime() != null && row.getPlaceName() != null) {
                    String placeName = translatedPlaceNames.getOrDefault(row.getTourId(), row.getPlaceName());
                    items.add(new MyTodayScheduleItemData(row.getTime(), placeName));
                }
            }

            List<MyTodayScheduleMoimData> data = new ArrayList<>();
            for (java.util.Map.Entry<String, String> entry : titleByMoimId.entrySet()) {
                data.add(new MyTodayScheduleMoimData(
                        entry.getKey(),
                        entry.getValue(),
                        itemsByMoimId.getOrDefault(entry.getKey(), new ArrayList<>())
                ));
            }

            return new MyTodayScheduleResponse(
                    true,
                    200,
                    "SUCCESS",
                    "오늘 진행 중인 모임 일정을 정상적으로 조회했습니다.",
                    "/moimList/myTodaySchedule",
                    "",
                    data
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new MyTodayScheduleResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/moimList/myTodaySchedule",
                    "",
                    null
            );
        }
    }

    //모임 생성
    @Override
    @Transactional
    public CreateMoimResponse createMoim(CreateMoimRequest request, String userId) {

        try {

            //모임ID 생성 (기존에는 DB 함수 FN_GET_MOIM_ID()를 호출했는데, 이 함수가 어느 스크립트에도
            //정의되어 있지 않아 실제 DB에 없을 가능성이 높고 소모임 생성 실패의 유력한 원인이었다.
            //다른 ID들(RVT/RVO 리뷰ID, UGC 관광지ID)과 동일하게 애플리케이션에서 직접 생성하도록 바꿔
            //DB 함수 존재 여부와 무관하게 항상 동작하게 한다)
            String moimId = "M" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

            //모임 등록
            moimListMapper.createMoimList(request, moimId, userId);

            //모임장 등록
            moimListMapper.insertMoimMember(moimId, userId, "A", "Y");

            //모임 테마 등록
            if (request.getMoimCateData() != null) {

                for (MoimCateData cateData : request.getMoimCateData()) {

                    moimListMapper.insertMoimCate(cateData, moimId, userId);
                }
            }

            //모임 일정 등록
            if (request.getMoimPlanData() != null) {

                for (MoimPlanInsertData moimPlan : request.getMoimPlanData()) {

                    moimListMapper.insertMoimPlan(moimPlan, moimId, userId);
                }

                //대표 이미지를 첫 일정 관광지 이미지로 설정
                moimListMapper.updateMoimImgFromFirstPlan(moimId);
            }

            //제목/소개는 등록 후 바뀌지 않는 정적 데이터라, 등록이 확정되면 영어/일본어로 한 번 번역해 캐시에
            //저장해 둔다(예전엔 외국어 사용자가 처음 볼 때 번역해서, 첫 조회가 느리고 그때 AI가 안 되면 한국어로 보였다).
            //응답을 늦추지 않도록 커밋 뒤 백그라운드에서 하고, 실패해도 조회 시 번역(기존 방식)이 대신 채운다.
            String title = request.getMoimTitle();
            String dscr = request.getMoimDscr();
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        CompletableFuture.runAsync(() -> pretranslateMoim(moimId, title, dscr));
                    }
                });
            }

            return new CreateMoimResponse(
                    true,
                    200,
                    "SUCCESS",
                    "모임 생성 성공",
                    "/moimList/createMoim",
                    new CreateMoimData(moimId)
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            //예외를 잡아 응답으로 바꾸면 @Transactional이 롤백하지 않아, 중간에 실패하면 모임 행만 남은
            //반쪽짜리 모임(모임장/테마 없음)이 생길 수 있었다. 응답은 그대로 두고 트랜잭션만 롤백시킨다.
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            return new CreateMoimResponse(
                    false,
                    500,
                    "FAIL",
                    "모임 생성 중 오류가 발생했습니다.",
                    "/moimList/createMoim",
                    null
            );
        }
    }

    //등록한 모임의 제목/소개를 원문 외 언어(ko/en/ja)로 미리 번역해 캐시(MOIM_TITLE_*/MOIM_DSCR_*)에 저장한다.
    void pretranslateMoim(String moimId, String title, String dscr) {
        // 원문 언어(글자로 판별)를 뺀 나머지 언어로 번역한다. 한국어로 쓴 모임은 en/ja, 영어로 쓴 모임은 ko/ja.
        Map<String, String> fields = new java.util.LinkedHashMap<>();
        fields.put("title", title == null ? "" : title);
        fields.put("dscr", dscr == null ? "" : dscr);
        ContentTranslationHelper.pretranslate(fields, tourListService::translateFreeTexts,
                (lang, t) -> moimListMapper.updateMoimTranslationLang(moimId, lang.toUpperCase(), t.get("title"), t.get("dscr")));
    }

    // 모임 콘텐츠 한 항목(제목 또는 소개)의 표시값. 원문이 이미 그 언어면 원문, 아니면 그 언어 캐시 중
    // 검증을 통과한 값(대상 언어 문자로 쓰인 번역), 둘 다 아니면 null(번역 필요).
    static String moimDisplayFromCache(String source, String lang, String ko, String en, String ja) {
        return ContentLangUtil.displayFromCache(source, lang, ko, en, ja);
    }

    // key("모임ID#title" / "모임ID#dscr")별 원문을 한 번에 번역하고, 성공한 것만 모임별로 그 언어 캐시에 저장한다.
    // 번역 실패/AI 불가면 빈 결과를 돌려주고(호출부는 원문 표시), 예외를 밖으로 던지지 않는다.
    private Map<String, String> translateAndCacheMoims(Map<String, String> toTranslate, String lang) {
        if (toTranslate == null || toTranslate.isEmpty() || !ContentLangUtil.isSupported(lang)) return Map.of();
        try {
            Map<String, String> translated = tourListService.translateFreeTexts(toTranslate, lang);
            Map<String, String[]> byMoim = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, String> entry : translated.entrySet()) {
                String key = entry.getKey();
                if (!toTranslate.containsKey(key)) continue;
                int sep = key.lastIndexOf('#');
                String moimId = key.substring(0, sep);
                String[] values = byMoim.computeIfAbsent(moimId, k -> new String[2]);
                if (key.endsWith("#title")) values[0] = entry.getValue(); else values[1] = entry.getValue();
            }
            byMoim.forEach((moimId, v) -> moimListMapper.updateMoimTranslationLang(moimId, lang.toUpperCase(), v[0], v[1]));
            return translated;
        } catch (Exception e) {
            log.warn("소모임 제목/소개 번역에 실패해 원문으로 표시합니다. lang={}", lang, e);
            return Map.of();
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    //모임 신청
    @Override
    @Transactional
    public ApplyMoimResponse applyMoim(String moimId, String userId) {

        try {
            // 신청 버튼을 빠르게 두 번 누르거나 네트워크 재시도로 요청이 겹치는 경우를
            // 막기 위해, INSERT 전에 이미 신청/가입된 상태인지 먼저 확인한다. 최종적인
            // 동시성 보장은 DB의 (MOIM_ID, USER_ID) 유니크 제약이 하지만, 여기서 미리
            // 걸러내면 사용자에게 더 친절한 메시지를 보여줄 수 있다.
            // 프론트는 본인 모임이면 신청 버튼 자체를 숨기지만, API를 직접 호출하면
            // 우회할 수 있어 서버에서도 막는다.
            if (userId.equals(moimListMapper.moimHostUserId(moimId))) {
                return new ApplyMoimResponse(
                        false,
                        400,
                        "SELF_APPLY",
                        "본인이 만든 모임에는 신청할 수 없습니다.",
                        "/moimList/" + moimId + "/apply",
                        ""
                );
            }

            MoimJoinStatusData existing = moimListMapper.moimJoinStatus(moimId, userId);
            if (existing != null) {
                return new ApplyMoimResponse(
                        false,
                        409,
                        "ALREADY_APPLIED",
                        "이미 신청했거나 가입된 모임입니다.",
                        "/moimList/" + moimId + "/apply",
                        ""
                );
            }

            // 예전에는 정원을 승인 단계에서만 확인해서, 이미 꽉 찬 모임에도 신청이 계속
            // 쌓였다(모임장은 승인도 못 하는 신청을 계속 받게 됨). 신청 단계에서도 막는다.
            // 정원 값이 없거나 0 이하인 예전 데이터는 제한 없음으로 본다(모두 "꽉 참"으로 막히지 않게).
            Integer maxMember = moimListMapper.moimMaxMemberForUpdate(moimId);
            if (maxMember != null && maxMember > 0 && moimListMapper.approvedMemberCount(moimId) >= maxMember) {
                return new ApplyMoimResponse(
                        false,
                        409,
                        "MOIM_FULL",
                        "모임 정원이 가득 찼습니다.",
                        "/moimList/" + moimId + "/apply",
                        ""
                );
            }

            moimListMapper.insertMoimMember(moimId, userId, "M", "N");
            notificationMapper.insertApplyNotification(moimId, userId);

            // 실시간 알림 푸시는 부가 기능이라 여기서 실패해도 신청 자체는 이미
            // 완료된 것으로 처리해야 하므로, 별도로 감싸서 신청 성공 여부에 영향을 주지 않게 한다.
            try {
                notificationPushService.pushToUser(moimListMapper.moimHostUserId(moimId));
            } catch (Exception e) {
                log.warn("모임장 실시간 알림 푸시에 실패했습니다. moimId={}", moimId, e);
            }

            return new ApplyMoimResponse(
                    true,
                    200,
                    "SUCCESS",
                    "모임 신청이 완료되었습니다.",
                    "/moimList/" + moimId + "/apply",
                    ""
            );
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 위 사전 체크와 실제 INSERT 사이의 짧은 틈에 두 요청이 동시에 들어온 경우.
            // DB가 걸러준 것이므로 데이터는 안전하고, 사용자에게는 친절한 메시지만 보여준다.
            log.warn("모임 신청이 중복 요청되었습니다. moimId={}, userId={}", moimId, userId);
            return new ApplyMoimResponse(
                    false,
                    409,
                    "ALREADY_APPLIED",
                    "이미 신청했거나 가입된 모임입니다.",
                    "/moimList/" + moimId + "/apply",
                    ""
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new ApplyMoimResponse(
                    false,
                    500,
                    "FAIL",
                    "모임 신청 중 오류가 발생했습니다.",
                    "/moimList/" + moimId + "/apply",
                    ""
            );
        }
    }

    //모임 삭제(소프트 삭제, 방장 본인만 가능)
    @Override
    public DeleteMoimResponse deleteMoim(String moimId, String userId) {

        try {
            // UPDATE 조건 자체에 CREATE_USER = userId를 포함시켜, 영향받은 행이
            // 0이면 "존재하지 않거나 본인 소유가 아님"으로 한 번에 판단한다(별도
            // 조회 후 삭제하는 TOCTOU 틈을 만들지 않는다).
            int updated = moimListMapper.deleteMoim(moimId, userId);
            if (updated == 0) {
                return new DeleteMoimResponse(
                        false,
                        403,
                        "FORBIDDEN",
                        "본인이 만든 모임만 삭제할 수 있습니다.",
                        "/moimList/" + moimId,
                        ""
                );
            }

            // 모임이 사라졌는데 채팅방 멤버로는 계속 남아있으면, 채팅 목록에 더 이상
            // 존재하지 않는 모임의 대화방이 유령처럼 남아있게 된다. 멤버 전원을
            // 채팅방에서 내보내(메시지 자체는 남겨둔 채) 채팅 목록에서 사라지게 한다.
            try {
                chatMapper.deleteAllChatMembersByRoom("moim-" + moimId);
            } catch (Exception e) {
                log.warn("모임 삭제 후 채팅방 멤버 정리에 실패했습니다. moimId={}", moimId, e);
            }

            return new DeleteMoimResponse(
                    true,
                    200,
                    "SUCCESS",
                    "모임이 삭제되었습니다.",
                    "/moimList/" + moimId,
                    ""
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new DeleteMoimResponse(
                    false,
                    500,
                    "FAIL",
                    "모임 삭제 중 오류가 발생했습니다.",
                    "/moimList/" + moimId,
                    ""
            );
        }
    }

    //모임 멤버 목록 조회
    @Override
    public MoimMembersResponse moimMembers(String moimId) {

        try {
            List<MoimMemberData> members = moimListMapper.moimMembers(moimId);

            return new MoimMembersResponse(
                    true,
                    200,
                    "SUCCESS",
                    "모임 멤버 목록을 정상적으로 조회했습니다.",
                    "/moimList/" + moimId + "/members",
                    "",
                    members
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new MoimMembersResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/moimList/" + moimId + "/members",
                    "",
                    null
            );
        }
    }

    //모임 멤버 상태 변경(승인/거절)
    @Override
    @Transactional
    public UpdateMoimMemberResponse updateMoimMember(String moimId, String targetUserId, UpdateMoimMemberRequest request, String userId) {

        try {
            // 신청 승인/거절뿐 아니라 이미 가입된 멤버 추방(거절과 동일하게 행을 삭제)에도
            // 쓰이는 엔드포인트라, 모임장이 아닌 사람이 함부로 멤버를 바꾸지 못하도록 막는다.
            if (!userId.equals(moimListMapper.moimHostUserId(moimId))) {
                return new UpdateMoimMemberResponse(
                        false,
                        403,
                        "FORBIDDEN",
                        "모임장만 처리할 수 있습니다.",
                        "/moimList/" + moimId + "/members/" + targetUserId,
                        ""
                );
            }

            if (request.isApprove()) {
                // 정원 체크 없이 승인만 되던 구멍을 막는다 — 특히 "전체선택 후 일괄승인"에서
                // 한 번에 정원을 넘겨버릴 수 있었다. 이미 승인된 멤버 재승인(멱등 처리)은
                // 정원 계산에서 제외해 막히지 않게 한다.
                // FOR UPDATE로 모임 행을 잠가서, 여러 신청을 동시에(예: 전체선택 일괄승인)
                // 승인해도 정원 체크가 한 번에 하나씩만 통과하게 한다.
                Integer maxMember = moimListMapper.moimMaxMemberForUpdate(moimId);
                if (maxMember != null) {
                    MoimJoinStatusData targetStatus = moimListMapper.moimJoinStatus(moimId, targetUserId);
                    boolean alreadyApproved = targetStatus != null && "Y".equals(targetStatus.getStateCd());
                    if (!alreadyApproved && moimListMapper.approvedMemberCount(moimId) >= maxMember) {
                        return new UpdateMoimMemberResponse(
                                false,
                                409,
                                "MOIM_FULL",
                                "모임 정원이 가득 찼습니다.",
                                "/moimList/" + moimId + "/members/" + targetUserId,
                                ""
                        );
                    }
                }

                moimListMapper.updateMoimMemberState(moimId, targetUserId, "Y", userId);
                // 승인된 사람을 소모임 채팅방에도 자동으로 합류시킨다(모임 인원수와
                // 채팅방 인원수가 어긋나지 않도록). 이미 들어와 있으면 INSERT IGNORE라 아무
                // 일도 안 하고, 예전에 추방됐다가 재승인된 경우엔 상태를 정상으로 되돌린다.
                String roomId = "moim-" + moimId;
                chatMapper.insertRoomIfNotExists(roomId, chatMapper.moimTitle(moimId), targetUserId);
                chatMapper.insertMemberIfNotExists(roomId, targetUserId);
                chatMapper.reactivateChatMember(roomId, targetUserId);
            } else {
                moimListMapper.deleteMoimMember(moimId, targetUserId);
                // 채팅방은 그대로 남겨두고 상태만 "추방됨"으로 표시한다(대기 중이던
                // 신청자를 거절하는 경우엔 애초에 채팅방 멤버 행이 없을 수 있는데, 그때는
                // 그냥 아무 일도 일어나지 않는다).
                chatMapper.markChatMemberKicked("moim-" + moimId, targetUserId);
            }

            return new UpdateMoimMemberResponse(
                    true,
                    200,
                    "SUCCESS",
                    request.isApprove() ? "신청을 승인했습니다." : "신청을 거절했습니다.",
                    "/moimList/" + moimId + "/members/" + targetUserId,
                    ""
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new UpdateMoimMemberResponse(
                    false,
                    500,
                    "FAIL",
                    "처리 중 오류가 발생했습니다.",
                    "/moimList/" + moimId + "/members/" + targetUserId,
                    ""
            );
        }
    }

    //모임 일정 수정
    @Override
    @Transactional
    public UpdateMoimPlanResponse updateMoimPlan(String moimId, UpdateMoimPlanRequest request, String userId) {

        try {
            if (!userId.equals(moimListMapper.moimHostUserId(moimId))) {
                return new UpdateMoimPlanResponse(
                        false,
                        403,
                        "FORBIDDEN",
                        "모임장만 일정을 수정할 수 있습니다.",
                        "/moimList/" + moimId + "/plan"
                );
            }

            if (request.getMoimEndDt() != null && !request.getMoimEndDt().isBlank()) {
                MoimDetailData moim = moimListMapper.moimDetail(moimId);
                if (moim != null && moim.getMoimStartDt() != null
                        && java.time.LocalDate.parse(request.getMoimEndDt()).isBefore(moim.getMoimStartDt())) {
                    return new UpdateMoimPlanResponse(
                            false,
                            400,
                            "INVALID_DATE",
                            "종료일은 시작일보다 이전일 수 없습니다.",
                            "/moimList/" + moimId + "/plan"
                    );
                }
                moimListMapper.updateMoimEndDt(moimId, request.getMoimEndDt(), userId);
            }

            moimListMapper.deleteMoimPlan(moimId);
            if (request.getItems() != null) {
                for (MoimPlanInsertData item : request.getItems()) {
                    moimListMapper.insertMoimPlan(item, moimId, userId);
                }
            }

            //대표 이미지를 변경된 일정의 첫 관광지 이미지로 다시 설정
            moimListMapper.updateMoimImgFromFirstPlan(moimId);

            return new UpdateMoimPlanResponse(
                    true,
                    200,
                    "SUCCESS",
                    "일정을 수정했습니다.",
                    "/moimList/" + moimId + "/plan"
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new UpdateMoimPlanResponse(
                    false,
                    500,
                    "FAIL",
                    "일정 수정 중 오류가 발생했습니다.",
                    "/moimList/" + moimId + "/plan"
            );
        }
    }

    //모임 관리(신청자 목록) 조회 시 가입 신청 알림 읽음 처리
    @Override
    public MarkApplicantsReadResponse markApplicantsRead(String moimId, String userId) {

        try {
            notificationMapper.markApplyNotificationsRead(moimId, userId);

            return new MarkApplicantsReadResponse(
                    true,
                    200,
                    "SUCCESS",
                    "알림을 읽음 처리했습니다.",
                    "/moimList/" + moimId + "/applicantsRead"
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new MarkApplicantsReadResponse(
                    false,
                    500,
                    "FAIL",
                    "처리 중 오류가 발생했습니다.",
                    "/moimList/" + moimId + "/applicantsRead"
            );
        }
    }

    //모임(여행) 후기 등록
    @Override
    public CreateMoimReviewResponse createMoimReview(String moimId, CreateMoimReviewRequest request, String userId) {

        try {
            // 여행이 아직 끝나지 않은(종료일이 지나지 않은) 소모임에는 후기를 남길 수
            // 없다. 검증 없이 등록만 됐던 걸 막는다.
            MoimSearchData moimInfo = moimListMapper.moimInfo(moimId);
            if (moimInfo == null || moimInfo.getMoimEndDt() == null
                    || !moimInfo.getMoimEndDt().isBefore(java.time.LocalDate.now())) {
                return new CreateMoimReviewResponse(
                        false,
                        400,
                        "MOIM_NOT_FINISHED",
                        "여행이 끝난 후에 후기를 남길 수 있습니다.",
                        "/moimList/" + moimId + "/review",
                        ""
                );
            }

            // 모임에 참여하지 않은 사람도 모임ID만 알면 후기를 남길 수 있었다. 모임장 또는
            // 승인된(Y) 멤버만 후기를 쓸 수 있게 한다.
            MoimJoinStatusData joinStatus = moimListMapper.moimJoinStatus(moimId, userId);
            boolean isParticipant = userId.equals(moimListMapper.moimHostUserId(moimId))
                    || (joinStatus != null && "Y".equals(joinStatus.getStateCd()));
            if (!isParticipant) {
                return new CreateMoimReviewResponse(
                        false,
                        403,
                        "NOT_MEMBER",
                        "모임에 참여한 멤버만 후기를 남길 수 있습니다.",
                        "/moimList/" + moimId + "/review",
                        ""
                );
            }

            String imgUrls = request.getImageUrls() == null ? null : String.join(",", request.getImageUrls());
            String reviewId = "RVO" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            moimListMapper.insertMoimReview(reviewId, moimId, request, imgUrls, userId);
            // 후기는 등록 후 바뀌지 않으므로 원문 외 언어로 한 번 번역해 저장해 둔다(커밋 후 백그라운드, 실패해도 등록은 성공).
            String reviewContent = request.getReviewContent();
            ContentTranslationHelper.runAfterCommit(() -> ContentTranslationHelper.pretranslate(
                    Map.of("content", reviewContent == null ? "" : reviewContent),
                    tourListService::translateFreeTexts,
                    (lang, t) -> moimListMapper.updateReviewTranslationLang(reviewId, lang.toUpperCase(), null, t.get("content"))));

            return new CreateMoimReviewResponse(
                    true,
                    200,
                    "SUCCESS",
                    "후기 등록이 완료되었습니다.",
                    "/moimList/" + moimId + "/review",
                    ""
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new CreateMoimReviewResponse(
                    false,
                    500,
                    "FAIL",
                    "후기 등록 중 오류가 발생했습니다.",
                    "/moimList/" + moimId + "/review",
                    ""
            );
        }
    }

    //모임(여행) 후기 목록 조회
    @Override
    public MoimReviewsResponse moimReviews(String moimId, String lang) {

        try {
            List<MoimReviewData> reviews = moimListMapper.moimReviews(moimId);
            // moimDetail과 같은 이유로, 캐시가 없는 후기 번역이 응답을 오래 붙잡지
            // 않도록 최대 0.5초만 기다린다. 못 끝나면 한국어로 먼저 보여주고, 번역은
            // 백그라운드에서 계속 돌아 캐시에 저장된다.
            try {
                CompletableFuture.runAsync(() -> applyMoimReviewTranslations(reviews, lang))
                        .get(500, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                log.warn("모임 후기 번역이 0.5초 안에 끝나지 않아 한국어로 먼저 응답합니다. moimId={}, lang={}", moimId, lang);
            } catch (Exception e) {
                log.warn("모임 후기 번역 대기 중 오류가 발생했습니다. moimId={}, lang={}", moimId, lang, e);
            }

            return new MoimReviewsResponse(
                    true,
                    200,
                    "SUCCESS",
                    "모임 후기 목록을 정상적으로 조회했습니다.",
                    "/moimList/" + moimId + "/reviews",
                    "",
                    reviews
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new MoimReviewsResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/moimList/" + moimId + "/reviews",
                    "",
                    null
            );
        }
    }

    //모임 일정 기반 교통편 혼잡도 분석(가입된 멤버만)
    @Override
    public TransportRecommendResponse moimTransportRecommend(String moimId, String userId, String lang) {

        if (userId == null) {
            return new TransportRecommendResponse(
                    false,
                    500,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/moimList/" + moimId + "/transportRecommend",
                    "",
                    null
            );
        }

        try {
            // 승인된(STATE_CD='Y') 멤버만 볼 수 있다. 모임장도 생성 시 ROLE_CD='A',
            // STATE_CD='Y'로 자기 자신이 멤버 테이블에 등록되므로 여기서 함께 걸러진다.
            MoimJoinStatusData joinStatus = moimListMapper.moimJoinStatus(moimId, userId);
            if (joinStatus == null || !"Y".equals(joinStatus.getStateCd())) {
                return new TransportRecommendResponse(
                        false,
                        403,
                        "FORBIDDEN",
                        "가입된 소모임 멤버만 교통편 분석을 볼 수 있습니다.",
                        "/moimList/" + moimId + "/transportRecommend",
                        "",
                        null
                );
            }

            List<MoimPlanData> plan = moimListMapper.moimPlan(moimId, "ko");

            // 일정은 달력 날짜(START_DT)로 저장돼 있는데, AI 프롬프트에는 실제 날짜가
            // 아니라 "몇 번째 날"(1일차, 2일차...)이 필요하므로 날짜를 오름차순으로
            // 정렬해 순번을 매긴다.
            List<String> sortedDates = plan.stream()
                    .map(MoimPlanData::getStartDt)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .sorted()
                    .toList();
            java.util.Map<String, Integer> dayIndexByDate = new java.util.HashMap<>();
            for (int i = 0; i < sortedDates.size(); i++) {
                dayIndexByDate.put(sortedDates.get(i), i + 1);
            }

            List<TransportRecommendRequest.TransportStopInput> stops = new ArrayList<>();
            for (MoimPlanData item : plan) {
                Integer day = dayIndexByDate.get(item.getStartDt());
                if (day == null || item.getTourId() == null) continue;

                TransportRecommendRequest.TransportStopInput stop = new TransportRecommendRequest.TransportStopInput();
                stop.setDay(day);
                stop.setTime(item.getRmks());
                stop.setTourId(item.getTourId());
                stop.setTourNm(item.getTourNm());
                stop.setRoadAddr(item.getRoadAddr());
                stops.add(stop);
            }

            TransportRecommendRequest request = new TransportRecommendRequest();
            request.setItems(stops);
            request.setLang(lang);

            TransportRecommendResponse result = tourListService.transportRecommend(request);

            return new TransportRecommendResponse(
                    result.isSuccess(),
                    result.getStatus(),
                    result.getCode(),
                    result.getMessage(),
                    "/moimList/" + moimId + "/transportRecommend",
                    result.getToken(),
                    result.getData()
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new TransportRecommendResponse(
                    false,
                    500,
                    "FAIL",
                    "교통편 분석 중 오류가 발생했습니다.",
                    "/moimList/" + moimId + "/transportRecommend",
                    "",
                    null
            );
        }
    }

    // ========================= 소모임 제목/소개/후기 번역 =========================
    // 사용자가 직접 입력하는 값이라 한국어만 존재하므로, 관광지와 동일한 방식으로
    // 제목/소개는 DB에 캐시하고(재사용), 계속 새로 작성되는 후기는 조회 시점에 매번 번역한다.

    // Gemini 호출 시 주고받은 JSON을 그대로 콘솔에 남긴다. AI 응답이 비거나 이상할 때
    // 이 로그를 보면 실제로 어떤 프롬프트를 보냈고 Gemini가 뭐라고 답했는지 바로 확인할 수 있다.
    private GeminiResponse callGemini(GeminiRequest geminiRequest) {
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println("[Gemini 요청] " + objectMapper.writeValueAsString(geminiRequest));

        // Gemini가 일시적으로 과부하(503)이거나 요청이 몰려 제한(429)에 걸리는 경우가 잦아,
        // 한 번 실패했다고 바로 포기하면 제목/후기 등 번역이 자주 안 되고 한국어 원문만
        // 보이는 문제가 있었다. 과부하성 오류에 한해 짧게 두 번 더 재시도한다.
        RestClientException lastError = null;
        // 사용량 한도 초과로 쉬는 중이면 호출하지 않고 바로 실패 처리한다.
        AiErrorUtil.throwIfQuotaBlocked();
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
                // 사용량 한도 초과(일일 한도, 긴 retryDelay)는 짧은 재시도로 풀리지 않으므로
                // 바로 포기하고, Gemini가 알려준 시간 동안 호출을 쉰다.
                long cooldownMillis = AiErrorUtil.quotaCooldownMillis(e);
                if (cooldownMillis > 0) {
                    AiErrorUtil.markQuotaBlocked(cooldownMillis);
                    log.warn("Gemini 사용량 한도를 초과해 {}초 동안 호출을 건너뜁니다.", cooldownMillis / 1000);
                    throw e;
                }
                if (attempt == 4 || !AiErrorUtil.isRetryable(e)) throw e;
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

    private void applyMoimDetailTranslation(MoimDetailData moim, String lang) {
        if (moim == null || !ContentLangUtil.isSupported(lang)) return;
        String title = moimDisplayFromCache(moim.getMoimTitle(), lang, moim.getMoimTitleKo(), moim.getMoimTitleEn(), moim.getMoimTitleJa());
        String dscr = moimDisplayFromCache(moim.getMoimDscr(), lang, moim.getMoimDscrKo(), moim.getMoimDscrEn(), moim.getMoimDscrJa());
        Map<String, String> need = new java.util.LinkedHashMap<>();
        if (title == null) need.put(moim.getMoimId() + "#title", moim.getMoimTitle());
        if (dscr == null) need.put(moim.getMoimId() + "#dscr", moim.getMoimDscr());
        Map<String, String> translated = translateAndCacheMoims(need, lang);
        moim.setMoimTitle(title != null ? title : translated.getOrDefault(moim.getMoimId() + "#title", moim.getMoimTitle()));
        moim.setMoimDscr(dscr != null ? dscr : translated.getOrDefault(moim.getMoimId() + "#dscr", moim.getMoimDscr()));
    }

    // moimPlan은 캐시된 번역(TOUR_NM_EN/JA)만 읽어오는데, 아직 그 언어로 한 번도
    // 조회된 적 없는 관광지는 캐시가 비어 있어 한국어 이름이 그대로 나온다. 여기서
    // 한 번 더 번역을 시도해 캐시를 채우고 화면에도 바로 반영한다.
    private void applyMoimPlanTranslation(List<MoimPlanData> moimPlan, String lang) {
        if (moimPlan == null || moimPlan.isEmpty()) return;

        // 공식 일어/영어 데이터나 Gemini 캐시로 이미 이름이 채워진 항목
        // (NATIVE_MATCH_YN='Y')은 다시 번역할 필요가 없다.
        List<String> tourIdsNeedingTranslation = moimPlan.stream()
                .filter(item -> !"Y".equals(item.getNativeMatchYn()))
                .map(MoimPlanData::getTourId)
                .toList();
        Map<String, String> translatedNames = tourListService.translateTourNames(tourIdsNeedingTranslation, lang);
        for (MoimPlanData item : moimPlan) {
            String translatedNm = translatedNames.get(item.getTourId());
            if (translatedNm != null && !translatedNm.isBlank()) {
                item.setTourNm(translatedNm);
            }
        }
    }

    // 관광지 후기와 동일하게 REVIEW_ID 기준으로 캐시한다.
    private void applyMoimReviewTranslations(List<MoimReviewData> reviews, String lang) {
        ContentTranslationHelper.apply(reviews, lang, new ContentTranslationHelper.Field<>(
                        MoimReviewData::getReviewId, MoimReviewData::getReviewContent,
                        MoimReviewData::getReviewContentKo, MoimReviewData::getReviewContentEn, MoimReviewData::getReviewContentJa,
                        MoimReviewData::setReviewContent),
                tourListService::translateFreeTexts,
                (id, text) -> moimListMapper.updateReviewTranslationLang(id, lang.toUpperCase(), null, text));
    }

    // 소모임 목록(moimSearch)에서 제목/설명을 함께 번역한다. moimSearch 쿼리가 이미
    // MOIM_TITLE_*/MOIM_DSCR_* 캐시 값을 같이 내려주므로 별도 조회 없이 바로 캐시를
    // 확인하고, 캐시가 없는 것만 모아 한 번의 Gemini 호출로 번역 후 캐시에 저장한다.
    private void applyMoimSearchTranslations(List<MoimSearchData> moims, String lang) {
        if (!ContentLangUtil.isSupported(lang) || moims == null || moims.isEmpty()) return;
        Map<String, String> need = new java.util.LinkedHashMap<>();
        Map<String, String> display = new HashMap<>();
        for (MoimSearchData moim : moims) {
            String title = moimDisplayFromCache(moim.getMoimTitle(), lang, moim.getMoimTitleKo(), moim.getMoimTitleEn(), moim.getMoimTitleJa());
            String dscr = moimDisplayFromCache(moim.getMoimDscr(), lang, moim.getMoimDscrKo(), moim.getMoimDscrEn(), moim.getMoimDscrJa());
            if (title == null) need.put(moim.getMoimId() + "#title", moim.getMoimTitle()); else display.put(moim.getMoimId() + "#title", title);
            if (dscr == null) need.put(moim.getMoimId() + "#dscr", moim.getMoimDscr()); else display.put(moim.getMoimId() + "#dscr", dscr);
        }
        display.putAll(translateAndCacheMoims(need, lang));
        for (MoimSearchData moim : moims) {
            moim.setMoimTitle(display.getOrDefault(moim.getMoimId() + "#title", moim.getMoimTitle()));
            moim.setMoimDscr(display.getOrDefault(moim.getMoimId() + "#dscr", moim.getMoimDscr()));
        }
    }

    //관광지의 translateTourNames와 동일한 용도로, 홈 화면처럼 소모임 제목만 필요한
    //다른 화면에서 재사용한다. MOIM_TITLE_* 캐시를 우선 쓰고, 없는 것만 모아
    //tourListService의 범용 텍스트 번역기로 한 번에 번역한 뒤 캐시에 저장한다.
    @Override
    public Map<String, String> translateMoimTitles(List<String> moimIds, String lang) {
        return translateMoimField(moimIds, lang, true);
    }

    //translateMoimTitles와 동일한 용도로, 홈 화면처럼 소모임 설명만 필요한 다른
    //화면에서 재사용한다. MOIM_DSCR_* 캐시를 우선 쓰고, 없는 것만 모아 번역한다.
    @Override
    public Map<String, String> translateMoimDescriptions(List<String> moimIds, String lang) {
        return translateMoimField(moimIds, lang, false);
    }

    // 모임ID별 제목(title=true) 또는 소개의 표시값(원문/검증된 캐시/새 번역 순, 실패 시 빠짐).
    private Map<String, String> translateMoimField(List<String> moimIds, String lang, boolean title) {
        if (!ContentLangUtil.isSupported(lang) || moimIds == null || moimIds.isEmpty()) return Map.of();
        List<String> distinctIds = moimIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (distinctIds.isEmpty()) return Map.of();

        String suffix = title ? "#title" : "#dscr";
        Map<String, String> result = new HashMap<>();
        Map<String, String> need = new java.util.LinkedHashMap<>();
        for (MoimTitleTranslationData moim : moimListMapper.moimTitlesByIds(distinctIds)) {
            String source = title ? moim.getMoimTitle() : moim.getMoimDscr();
            if (source == null || source.isBlank()) continue;
            String shown = title
                    ? moimDisplayFromCache(source, lang, moim.getMoimTitleKo(), moim.getMoimTitleEn(), moim.getMoimTitleJa())
                    : moimDisplayFromCache(source, lang, moim.getMoimDscrKo(), moim.getMoimDscrEn(), moim.getMoimDscrJa());
            if (shown != null) result.put(moim.getMoimId(), shown);
            else need.put(moim.getMoimId() + suffix, source);
        }
        translateAndCacheMoims(need, lang).forEach((key, value) -> result.put(key.substring(0, key.lastIndexOf('#')), value));
        return result;
    }
}
