package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// translateMoimTitles(번역 캐시 확인용)의 최소 조회 결과.
@Getter
@NoArgsConstructor
@Setter
public class MoimTitleTranslationData {
    private String moimId;
    private String moimTitle;
    private String moimTitleEn;
    private String moimTitleJa;
    private String moimDscr;
    private String moimDscrEn;
    private String moimDscrJa;
}
