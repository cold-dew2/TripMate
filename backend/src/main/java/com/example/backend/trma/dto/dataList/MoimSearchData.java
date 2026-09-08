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
    private LocalDate moimStartDt;
    private LocalDate moimEndDt;
    private String userNm;
    private Integer visitCnt;
    private Integer maxMember;
    private Integer memberCnt;
    private String cateCd;
    private String cateNm;
    private int avgScore;
}
