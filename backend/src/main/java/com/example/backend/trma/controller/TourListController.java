package com.example.backend.trma.controller;

import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.service.TourListService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tourList")
public class TourListController {
    private final TourListService tourListService;

    public TourListController(TourListService tourListService) {
        this.tourListService = tourListService;
    }

    //관광지 검색
    @GetMapping("/tourSearch")
    public TourSearchResponse tourSearch(@ModelAttribute TourSearchRequest request) {

        return tourListService.tourSearch(request);
    }

    //AI 관광지 추천
    @GetMapping("/tourAiSearch")
    public TourAiSearchResponse tourAiSearch(@ModelAttribute TourAiSearchRequest request) {

        return tourListService.tourAiSearch(request);
    }

    //관광지 상세조회(기본)
    @GetMapping("/tourDetail")
    public TourDetailResponse tourDetail(@ModelAttribute TourDetailRequest request) {

        return tourListService.tourDetail(request);
    }

    //관광지 상세조회(AI)
    @GetMapping("/tourAIDetail")
    public TourAiDetailResponse tourAIDetail(@ModelAttribute TourAiDetailRequest request) {

        return tourListService.tourAiDetail(request);
    }

    //관광지 상세조회(리뷰)
    @GetMapping("/tourDetailReview")
    public TourDetailReviewResponse tourDetailReview(@ModelAttribute TourDetailReviewRequest request) {

        return tourListService.tourDetailReview(request);
    }

    //관광지 후기 등록
    @PostMapping("/tourDetailReview")
    public CreateTourReviewResponse createTourReview(@RequestBody CreateTourReviewRequest request,
                                                      Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new CreateTourReviewResponse(
                    false,
                    500,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/tourList/tourDetailReview",
                    ""
            );
        }

        String userId = authentication.getName();
        return tourListService.createTourReview(request, userId);
    }

    //AI 일정 추천
    @PostMapping("/aiSchedule")
    public AiScheduleResponse aiSchedule(@RequestBody AiScheduleRequest request) {

        return tourListService.aiSchedule(request);
    }

    //사용자 관광지 등록(소모임 생성 시 직접 입력)
    @PostMapping("/customTour")
    public CustomTourResponse customTour(@RequestBody CustomTourRequest request,
                                          Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new CustomTourResponse(
                    false,
                    500,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/tourList/customTour",
                    "",
                    null
            );
        }

        String userId = authentication.getName();
        return tourListService.registerCustomTour(request, userId);
    }

    //일정별 교통편 추천
    @PostMapping("/transportRecommend")
    public TransportRecommendResponse transportRecommend(@RequestBody TransportRecommendRequest request) {

        return tourListService.transportRecommend(request);
    }

}
