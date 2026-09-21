package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class MoimPlanData {
    private String startDt;
    private String rmks;
    private String tourId;
    private String tourNm;
    private String roadAddr;
    private String firstImage;
    private String cateCd;
    private String cateNm;
    // 한국관광공사 공식 일어/영어 데이터 또는 Gemini 번역 캐시로 이미 이름이 채워졌으면 'Y'.
    private String nativeMatchYn;
}
