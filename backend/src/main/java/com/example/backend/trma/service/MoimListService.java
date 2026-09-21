package com.example.backend.trma.service;

import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;

import java.util.List;
import java.util.Map;

public interface MoimListService {
    //관광지 검색
    MoimSearchResponse moimSearch(MoimSearchRequest request);

    //AI 관광지 추천
    MoimAiSearchResponse moimAiSearch(MoimAiSearchRequest request);

    //모임 상세조회(기본)
    MoimDetailResponse moimDetail(MoimDetailRequest request, String userId);

    //내 모임 목록 조회
    MyMoimResponse myMoim(String userId, String lang);

    //내가 가입한 모임 중 오늘 진행 중인 모임들의 오늘 일정
    MyTodayScheduleResponse myTodaySchedule(String userId, String lang);

    //내 모임 목록 조회
    MoimCateSearchResponse moimCateSearch();

    //모임 생성
    CreateMoimResponse createMoim(CreateMoimRequest request, String userId);

    //모임 신청
    ApplyMoimResponse applyMoim(String moimId, String userId);

    //모임 삭제(방장 본인만 가능)
    DeleteMoimResponse deleteMoim(String moimId, String userId);

    //모임 멤버 목록 조회
    MoimMembersResponse moimMembers(String moimId);

    //모임 멤버 상태 변경(승인/거절)
    UpdateMoimMemberResponse updateMoimMember(String moimId, String targetUserId, UpdateMoimMemberRequest request, String userId);

    UpdateMoimPlanResponse updateMoimPlan(String moimId, UpdateMoimPlanRequest request, String userId);

    MarkApplicantsReadResponse markApplicantsRead(String moimId, String userId);

    //모임(여행) 후기 등록
    CreateMoimReviewResponse createMoimReview(String moimId, CreateMoimReviewRequest request, String userId);

    //모임(여행) 후기 목록 조회
    MoimReviewsResponse moimReviews(String moimId, String lang);

    //모임 일정 기반 교통편 혼잡도 분석(가입된 멤버만)
    TransportRecommendResponse moimTransportRecommend(String moimId, String userId, String lang);

    //소모임 제목 일괄 번역(캐시 우선, 없으면 번역 후 캐시에 저장). 홈 화면처럼 다른
    //화면에서 소모임 제목만 필요할 때 재사용한다(관광지의 translateTourNames와 동일한 용도).
    Map<String, String> translateMoimTitles(List<String> moimIds, String lang);

    //소모임 설명 일괄 번역(캐시 우선, 없으면 번역 후 캐시에 저장). translateMoimTitles와
    //동일한 용도로, 홈 화면처럼 다른 화면에서 소모임 설명만 필요할 때 재사용한다.
    Map<String, String> translateMoimDescriptions(List<String> moimIds, String lang);
}