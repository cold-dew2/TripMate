package com.example.backend.trma.service;

import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;

public interface TourListService {
    //관광지 검색
    TourSearchResponse tourSearch(TourSearchRequest request);

    //AI 관광지 추천
    TourAiSearchResponse tourAiSearch(TourAiSearchRequest request);

    //관광지 상세조회(기본)
    TourDetailResponse tourDetail(TourDetailRequest request);

    //관광지 상세조회(기본)
    TourAiDetailResponse tourAiDetail(TourAiDetailRequest request);

    //관광지 상세조회(리뷰)
    TourDetailReviewResponse tourDetailReview(TourDetailReviewRequest request);

}