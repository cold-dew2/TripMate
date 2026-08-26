package com.example.backend.trma.controller;

import com.example.backend.trma.dto.request.*;
import com.example.backend.trma.dto.response.*;
import com.example.backend.trma.service.MoimListService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/moimList")
public class MoimListController {
    private final MoimListService moimListService;

    public MoimListController(MoimListService moimListService) {
        this.moimListService = moimListService;
    }

    //모임 검색
    @GetMapping("/moimSearch")
    public MoimSearchResponse moimSearch(@ModelAttribute MoimSearchRequest request) {

        return moimListService.moimSearch(request);
    }

    //AI 모임 추천
    @GetMapping("/moimAiSearch")
    public MoimAiSearchResponse moimSearch(@ModelAttribute MoimAiSearchRequest request) {

        return moimListService.moimAiSearch(request);
    }

    //모임 상세조회(기본)
    @GetMapping("/moimDetail")
    public MoimDetailResponse moimDetail(@ModelAttribute MoimDetailRequest request,
                                         Authentication authentication) {

        String userId = authentication.getName();
        return moimListService.moimDetail(request, userId);
    }

    //내 모임 목록 조회
    @GetMapping("/myMoim")
    public MyMoimResponse myMoim(Authentication authentication) {

        String userId = authentication.getName();
        return moimListService.myMoim(userId);
    }

    //내 모임 목록 조회
    @GetMapping("/moimCateSearch")
    public MoimCateSearchResponse moimCateSearch() {

        return moimListService.moimCateSearch();
    }

    //모임 생성
    @PostMapping("/createMoim")
    public CreateMoimResponse createMoim(@RequestBody CreateMoimRequest request,
                                         Authentication authentication){
        String userId = authentication.getName();
        return moimListService.createMoim(request, userId);
    }
}
