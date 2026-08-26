package com.example.backend.trma.service;

import com.example.backend.trma.dto.request.MoimAiSearchRequest;
import com.example.backend.trma.dto.request.MoimDetailRequest;
import com.example.backend.trma.dto.request.MoimSearchRequest;
import com.example.backend.trma.dto.response.MoimAiSearchResponse;
import com.example.backend.trma.dto.response.MoimDetailResponse;
import com.example.backend.trma.dto.response.MoimSearchResponse;

public interface MoimListService {
    //관광지 검색
    MoimSearchResponse moimSearch(MoimSearchRequest request);

    //AI 관광지 추천
    MoimAiSearchResponse moimAiSearch(MoimAiSearchRequest request);

    //모임 상세조회(기본)
    MoimDetailResponse moimDetail(MoimDetailRequest request);
}