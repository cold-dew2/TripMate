package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class TourSearchData {
    private String tourId;
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
    private String firstImage;
    private String tourNmEn;
    private String tourNmJa;
    private String roadAddrEn;
    private String roadAddrJa;
    // 한국관광공사 공식 일어/영어 데이터로 이미 채워졌으면 'Y'.
    private String nativeMatchYn;
}
