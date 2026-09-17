package com.example.backend.trma.service;

import com.example.backend.trma.dto.response.TourAPIResponse;

public interface TourAPIService {
    //한국관광공사_기초지자체 중심 관광지 정보
//    TourAPIResponse areaBasedList();
    TourAPIResponse areaBased_batch();
    TourAPIResponse detailCommon_batch();
    TourAPIResponse searchFestival_batch();
    TourAPIResponse tourMaster_batch();
}
