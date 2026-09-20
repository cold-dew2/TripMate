package com.example.backend.trma.controller;

import com.example.backend.trma.dto.request.TourCategoryRequest;
import com.example.backend.trma.dto.request.UserInfoRequest;
import com.example.backend.trma.dto.response.*;
import org.springframework.security.core.Authentication;
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
    public UserInfoResponse userInfo(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new UserInfoResponse(
                    false,
                    500,
                    "NEED_LOGIN",
                    "로그인이 필요합니다.",
                    "/trmaHome/userInfo",
                    "",
                    null
            );
        }

        // 2. 정상 로그인된 사용자 처리
        String userId = authentication.getName();
        return trmaHomeService.userInfo(userId);
    }


    //알림
//    @GetMapping("/userInfo")
//    public UserInfoResponse userInfo(@ModelAttribute UserInfoRequest request) {
//
//        return trmaHomeService.userInfo(request);
//    }



    //공통코드(카테고리)
    //@GetMapping("/tourCategory")
    public TourCategoryResponse tourCategory(@ModelAttribute TourCategoryRequest request) {

        return trmaHomeService.tourCategory(request);
    }


    //인기 여행지(리뷰기반)
    @GetMapping("/bestTourList")
    public BestTourListResponse bestTourList(@RequestParam(required = false) String lang) {

        return trmaHomeService.bestTourList(lang);

    }


    //인기 모임(클릭 수 많은 모임)
    @GetMapping("/bestMoimList")
    public BestMoimListResponse bestMoimList(@RequestParam(required = false) String lang) {

        return trmaHomeService.bestMoimList(lang);

    }


}
