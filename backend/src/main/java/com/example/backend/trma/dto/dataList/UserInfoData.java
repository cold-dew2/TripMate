package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class UserInfoData {
    private String userNm;
    private String langCd;
    private String stateCd;
    private String roleCd;
    private String birthDt;
    private String genderCd;
    private String phoneNum;
}
