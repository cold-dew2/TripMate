package com.example.backend.trma.controller;

import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.service.TourListService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
