package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Setter
public class MoimSearchData {
    private String moimId;
    private String moimTitle;
    private String moimDscr;
    // 번역 캐시 확인용(응답에는 최종적으로 moimTitle/moimDscr에 반영된 값만 실려 나간다).
    private String moimTitleEn;
    private String moimTitleJa;
    private String moimDscrEn;
    private String moimDscrJa;
    private LocalDate moimStartDt;
    private LocalDate moimEndDt;
    private String userNm;
    private Integer visitCnt;
    private Integer maxMember;
    private Integer memberCnt;
    private String cateCd;
    private String cateNm;
    private int avgScore;
    private String imageUrl;
}
