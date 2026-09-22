package com.example.backend.trma.mapper;

import com.example.backend.trma.dto.dataList.TourAiDetailData;
import com.example.backend.trma.dto.dataList.TourAiSearchData;
import com.example.backend.trma.dto.dataList.TourCoordinateData;
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
    TourDetailData tourDetail(@Param("tourId") String tourId, @Param("lang") String lang);
    //관광지 이름 일괄 조회(번역 캐시 확인용)
    List<TourSearchData> tourByIds(@Param("tourIds") List<String> tourIds, @Param("lang") String lang);
    //관광지 좌표 일괄 조회(실시간 길찾기용)
    List<TourCoordinateData> tourCoordinatesByIds(@Param("tourIds") List<String> tourIds);
    //관광지 상세조회(리뷰)
    List<TourDetailReviewData> tourDetailReview(@Param("tourId") String tourId, @Param("offset") int offset);
    //관광지 후기 등록
    int insertTourReview(@Param("reviewId") String reviewId,
                         @Param("request") CreateTourReviewRequest request,
                         @Param("imgUrls") String imgUrls,
                         @Param("userId") String userId);
    //사용자 관광지 등록(소모임 생성 시 직접 입력)
    int insertCustomTour(@Param("tourId") String tourId,
                          @Param("request") CustomTourRequest request,
                          @Param("userId") String userId);
    //방금 등록한 관광지와 같은 주소를 쓰는 활성 관광지가 이미 있으면 DUP_YN='Y'로 표시
    //(검색 조회 시 매번 자기조인으로 계산하지 않도록, 등록 시점에 한 번만 계산해서 저장해둔다)
    int markDuplicateIfAddressExists(@Param("tourId") String tourId, @Param("roadAddr") String roadAddr);
    //관광지명/개요/주소 번역 캐시 저장
    int updateTourTranslation(@Param("tourId") String tourId,
                               @Param("tourNmEn") String tourNmEn,
                               @Param("tourNmJa") String tourNmJa,
                               @Param("overviewEn") String overviewEn,
                               @Param("overviewJa") String overviewJa,
                               @Param("roadAddrEn") String roadAddrEn,
                               @Param("roadAddrJa") String roadAddrJa,
                               @Param("detailAddrEn") String detailAddrEn,
                               @Param("detailAddrJa") String detailAddrJa);
    //관광지 후기 번역 캐시 저장
    int updateReviewTranslation(@Param("reviewId") String reviewId,
                                 @Param("reviewTitleEn") String reviewTitleEn,
                                 @Param("reviewTitleJa") String reviewTitleJa,
                                 @Param("reviewContentEn") String reviewContentEn,
                                 @Param("reviewContentJa") String reviewContentJa);
    //관광지 AI 이용정보 캐시 조회
    TourAiDetailData selectTourAiInfo(@Param("tourId") String tourId, @Param("langCd") String langCd);
    //관광지 AI 이용정보 캐시 저장(있으면 갱신)
    int upsertTourAiInfo(@Param("tourId") String tourId, @Param("langCd") String langCd, @Param("data") TourAiDetailData data);
}
