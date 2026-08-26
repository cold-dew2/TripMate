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
}