package com.example.backend.trma.service;

import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;

public interface MoimListService {
    //관광지 검색
    MoimSearchResponse moimSearch(MoimSearchRequest request);

    //AI 관광지 추천
    MoimAiSearchResponse moimAiSearch(MoimAiSearchRequest request);

    //모임 상세조회(기본)
    MoimDetailResponse moimDetail(MoimDetailRequest request, String userId);

    //내 모임 목록 조회
    MyMoimResponse myMoim(String userId);

    //내 모임 목록 조회
    MoimCateSearchResponse moimCateSearch();

    //모임 생성
    CreateMoimResponse createMoim(CreateMoimRequest request, String userId);

    //모임 신청
    ApplyMoimResponse applyMoim(String moimId, String userId);

    //모임 멤버 목록 조회
    MoimMembersResponse moimMembers(String moimId);

    //모임 멤버 상태 변경(승인/거절)
    UpdateMoimMemberResponse updateMoimMember(String moimId, String targetUserId, UpdateMoimMemberRequest request, String userId);

    //모임(여행) 후기 등록
    CreateMoimReviewResponse createMoimReview(String moimId, CreateMoimReviewRequest request, String userId);

    //모임(여행) 후기 목록 조회
    MoimReviewsResponse moimReviews(String moimId);
}