package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Setter
public class MoimDetailData {
    private String moimId;
    private String moimTitle;
    private String moimDscr;
    private LocalDate moimStartDt;
    private LocalDate moimEndDt;
    private Integer maxMember;
    private Integer memberCnt;
    private String userId;
    private String userNm;
    private int reviewScore;
    private String imageUrl;
    private String moimTitleEn;
    private String moimTitleJa;
    private String moimDscrEn;
    private String moimDscrJa;
}
