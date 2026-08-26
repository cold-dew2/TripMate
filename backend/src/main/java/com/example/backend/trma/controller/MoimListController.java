package com.example.backend.trma.controller;

import com.example.backend.trma.dto.request.MoimAiSearchRequest;
import com.example.backend.trma.dto.request.MoimDetailRequest;
import com.example.backend.trma.dto.request.MoimSearchRequest;
import com.example.backend.trma.dto.response.MoimAiSearchResponse;
import com.example.backend.trma.dto.response.MoimDetailResponse;
import com.example.backend.trma.dto.response.MoimSearchResponse;
import com.example.backend.trma.service.MoimListService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public MoimDetailResponse moimDetail(@ModelAttribute MoimDetailRequest request) {

        return moimListService.moimDetail(request);
    }
}
