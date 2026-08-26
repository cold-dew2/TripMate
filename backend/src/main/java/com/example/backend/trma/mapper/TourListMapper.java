package com.example.backend.trma.mapper;

import com.example.backend.trma.dto.dataList.TourAiSearchData;
import com.example.backend.trma.dto.dataList.TourDetailData;
import com.example.backend.trma.dto.dataList.TourDetailReviewData;
import com.example.backend.trma.dto.dataList.TourSearchData;
import com.example.backend.trma.dto.request.TourSearchRequest;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TourListMapper {
    //관광지 정보 조회
    List<TourSearchData> tourSearch(TourSearchRequest request);
    //관광지 상세
    TourSearchData tourInfo(String tourId);
    //관광지 상세조회(기본)
    TourDetailData tourDetail(String tourId);
    //관광지 상세조회(리뷰)
    List<TourDetailReviewData> tourDetailReview(String tourId);
}
