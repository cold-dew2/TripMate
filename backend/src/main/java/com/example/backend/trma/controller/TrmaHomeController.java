package com.example.backend.trma.controller;

import com.example.backend.trma.dto.request.TourCategoryRequest;
import com.example.backend.trma.dto.request.UserInfoRequest;
import com.example.backend.trma.dto.response.*;
import org.springframework.web.bind.annotation.*;
import com.example.backend.trma.service.TrmaHomeService;

@RestController
@RequestMapping("/trmaHome")

public class TrmaHomeController {
    private final TrmaHomeService trmaHomeService;

    public TrmaHomeController(TrmaHomeService trmaHomeService) {
        this.trmaHomeService = trmaHomeService;
    }

    //사용자 정보 조회
    @GetMapping("/userInfo")
    public UserInfoResponse userInfo(@ModelAttribute UserInfoRequest request) {

        return trmaHomeService.userInfo(request);
    }


    //알림
//    @GetMapping("/userInfo")
//    public UserInfoResponse userInfo(@ModelAttribute UserInfoRequest request) {
//
//        return trmaHomeService.userInfo(request);
//    }



    //공통코드(카테고리)
    @GetMapping("/tourCategory")
    public TourCategoryResponse tourCategory(@ModelAttribute TourCategoryRequest request) {

        return trmaHomeService.tourCategory(request);
    }


    //인기 여행지(리뷰기반)
    @GetMapping("/bestTourList")
    public BestTourListResponse bestTourList() {

        return trmaHomeService.bestTourList();

    }


    //인기 모임(클릭 수 많은 모임)
    @GetMapping("/bestMoimList")
    public BestMoimListResponse bestMoimList() {

        return trmaHomeService.bestMoimList();

    }


}
