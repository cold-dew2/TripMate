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

    //관광지 후기 등록
    CreateTourReviewResponse createTourReview(CreateTourReviewRequest request, String userId);

    //AI 일정 추천
    AiScheduleResponse aiSchedule(AiScheduleRequest request);

    //사용자 관광지 등록(소모임 생성 시 직접 입력)
    CustomTourResponse registerCustomTour(CustomTourRequest request, String userId);

    //일정별 교통편 추천
    TransportRecommendResponse transportRecommend(TransportRecommendRequest request);

}