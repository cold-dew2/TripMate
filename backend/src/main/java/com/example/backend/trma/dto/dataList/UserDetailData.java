package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class UserDetailData {
    private String userNm;
    private int moimCnt;
    private int moimMemberCnt;
    private int moimRevireCnt;
    private String langNm;
}
