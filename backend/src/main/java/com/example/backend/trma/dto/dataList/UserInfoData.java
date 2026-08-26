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
    private String langNm;
    private String stateCd;
    private String stateNm;
    private String roleCd;
    private String roleNm;
    private String birthDt;
    private String genderCd;
    private String genderNm;
    private String phoneNum;
}
