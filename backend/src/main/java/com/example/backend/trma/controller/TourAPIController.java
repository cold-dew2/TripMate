package com.example.backend.trma.controller;

import com.example.backend.trma.dto.request.TourSearchRequest;
import com.example.backend.trma.dto.response.TourAPIResponse;
import com.example.backend.trma.dto.response.TourSearchResponse;
import com.example.backend.trma.service.TourAPIService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tourAPI")
public class TourAPIController {

    private final TourAPIService tourAPIService;

    public TourAPIController(TourAPIService tourAPIService) {this.tourAPIService = tourAPIService;}

//    //한국관광공사_기초지자체 중심 관광지 정보
//    @GetMapping("/areaBasedList")
//    public TourAPIResponse areaBasedList() {
//
//        return tourAPIService.areaBasedList();
//    }

    //한국관광공사_국문 관광정보 서비스_GW
    @GetMapping("/areaBased_batch")
    public TourAPIResponse areaBased_batch() {

        return tourAPIService.areaBased_batch();
    }

    //한국관광공사_국문 관광정보 서비스_GW
    @GetMapping("/detailCommon_batch")
    public TourAPIResponse detailCommon_batch() {

        return tourAPIService.detailCommon_batch();
    }

    //한국관광공사_국문 관광정보 서비스_GW
    @GetMapping("/searchFestival_batch")
    public TourAPIResponse searchFestival_batch() {

        return tourAPIService.searchFestival_batch();
    }

    //한국관광공사_국문 관광정보 서비스_GW
    @GetMapping("/tourMaster_batch")
    public TourAPIResponse tourMaster_batch() {

        return tourAPIService.tourMaster_batch();
    }
}
