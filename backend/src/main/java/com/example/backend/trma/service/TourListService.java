package com.example.backend.trma.service;

import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;

import java.util.List;
import java.util.Map;

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

    //관광지 이름 일괄 번역(캐시 우선, 없으면 번역 후 캐시에 저장). 소모임 일정처럼 다른
    //화면에서 관광지 이름만 필요할 때 재사용한다.
    Map<String, String> translateTourNames(List<String> tourIds, String lang);

    //임의의 짧은 텍스트들을 key로 구분해 한 번에 번역(캐시 없이 즉시 번역만 수행).
    //호출하는 쪽에서 캐시 저장/조회를 직접 처리한다(예: 프로필 지역/소개글).
    Map<String, String> translateFreeTexts(Map<String, String> textsByKey, String lang);

}