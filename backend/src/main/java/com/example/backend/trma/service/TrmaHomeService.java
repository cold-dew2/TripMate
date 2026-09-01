package com.example.backend.trma.service;

import com.example.backend.trma.dto.request.TourCategoryRequest;
import com.example.backend.trma.dto.request.UserInfoRequest;
import com.example.backend.trma.dto.response.BestMoimListResponse;
import com.example.backend.trma.dto.response.BestTourListResponse;
import com.example.backend.trma.dto.response.TourCategoryResponse;
import com.example.backend.trma.dto.response.UserInfoResponse;

public interface TrmaHomeService {
    //사용자 정보 조회
    UserInfoResponse userInfo(String request);

    //공통코드(카테고리)
    TourCategoryResponse tourCategory(TourCategoryRequest request);

    //인기 모임(클릭 수 많은 모임)
    BestTourListResponse bestTourList();

    //인기 모임(클릭 수 많은 모임)
    BestMoimListResponse bestMoimList();
}
