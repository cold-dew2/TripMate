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
