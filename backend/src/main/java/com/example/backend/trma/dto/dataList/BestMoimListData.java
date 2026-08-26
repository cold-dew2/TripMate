package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class BestMoimListData {
    private String moimId;
    private String moinTitle;
    private String moinDscr;
    private String moimStartDt;
    private String moimEndDt;
    private String userId;
    private String userNm;
    private String cateCd;
    private String cateNm;
    private String memberCnt;
    private int visitCnt;
}
