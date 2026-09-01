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
}
