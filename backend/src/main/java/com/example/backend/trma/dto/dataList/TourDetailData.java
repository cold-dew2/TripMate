package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class TourDetailData {
    private String tourId;
    private String firstImage;
    private String tourNm;
    private String sidoCd;
    private String sidoNm;
    private String sggCd;
    private String sggNm;
    private String roadAddr;
    // 지도가 좌표(latitude/longitude)를 못 받았을 때 카카오 지오코더로 주소를 검색해
    // 대신 보여주는데, roadAddr은 화면 언어에 따라 영어/일본어로 번역돼 있어 그
    // 지오코더가 인식하지 못한다(한국 주소 검색 전용). 항상 한국어 원문 주소를 따로
    // 들고 있어야 지도가 제대로 뜬다.
    private String roadAddrKo;
    private String detailAddr;
    private String zipCd;
    private String cateCd;
    private String cateNm;
    private int avgScore;
    private String overview;
    private Double latitude;
    private Double longitude;
    private String tourNmEn;
    private String tourNmJa;
    private String overviewEn;
    private String overviewJa;
    private String roadAddrEn;
    private String roadAddrJa;
    private String detailAddrEn;
    private String detailAddrJa;
    // 한국관광공사 공식 일어/영어 데이터(TB_TRMA_TOUR_JP_LIST/EN_LIST)로 이미 채워졌으면 'Y'.
    // 이 경우 서비스 계층에서 Gemini 번역을 다시 시도하지 않는다.
    private String nativeMatchYn;
}
