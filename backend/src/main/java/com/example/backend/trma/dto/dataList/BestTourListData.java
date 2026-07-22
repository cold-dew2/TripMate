package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class BestTourListData {
    private String tourId;
    private String tourNm;
    private String sidoCd;
    private String sidoNm;
    private String sggCd;
    private String sggNm;
    private String emdCd;
    private String emdNm;
    private String roadAddr;
    private String detailAddr;
    private String zipCd;
    private int avgScore;
}
