package com.example.backend.trma.mapper;

import com.example.backend.trma.dto.dataList.TourAiSearchData;
import com.example.backend.trma.dto.dataList.TourDetailData;
import com.example.backend.trma.dto.dataList.TourDetailReviewData;
import com.example.backend.trma.dto.dataList.TourSearchData;
import com.example.backend.trma.dto.request.CreateTourReviewRequest;
import com.example.backend.trma.dto.request.CustomTourRequest;
import com.example.backend.trma.dto.request.TourSearchRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
    List<TourDetailReviewData> tourDetailReview(@Param("tourId") String tourId, @Param("offset") int offset);
    //관광지 후기 등록
    int insertTourReview(@Param("request") CreateTourReviewRequest request,
                         @Param("imgUrls") String imgUrls,
                         @Param("userId") String userId);
    //사용자 관광지 등록(소모임 생성 시 직접 입력)
    int insertCustomTour(@Param("tourId") String tourId,
                          @Param("request") CustomTourRequest request,
                          @Param("userId") String userId);
    //관광지명/개요 번역 캐시 저장
    int updateTourTranslation(@Param("tourId") String tourId,
                               @Param("tourNmEn") String tourNmEn,
                               @Param("tourNmJa") String tourNmJa,
                               @Param("overviewEn") String overviewEn,
                               @Param("overviewJa") String overviewJa);
}
