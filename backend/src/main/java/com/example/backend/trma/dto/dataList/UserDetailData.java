package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class UserDetailData {
    private String userNm;
    private String areaNm;
    private String description;
    private String profileImgUrl;
    private double rating;
    private int moimCnt;
    private int moimMemberCnt;
    private int moimRevireCnt;
    private String langCd;
    private String langNm;
    private String joinDt;
}
