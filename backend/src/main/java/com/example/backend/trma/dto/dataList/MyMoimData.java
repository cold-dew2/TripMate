package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Setter
public class MyMoimData {
    private String moimId;
    private String moimTitle;
    private String moimDscr;
    private LocalDate moimStartDt;
    private LocalDate moimEndDt;
    private Integer maxMember;
    private Integer memberCnt;
    private String roleCd;
    private String stateCd;
    private String cateCd;
    private String cateNm;
    private String reviewedYn;
}
