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
    // 번역 캐시(응답에는 그대로 노출되지만, 화면에서는 areaNm/description만 씀)
    private String areaNmEn;
    private String areaNmJa;
    private String descriptionEn;
    private String descriptionJa;
    private String profileImgUrl;
    private double rating;
    private int moimCnt;
    private int moimMemberCnt;
    private int moimRevireCnt;
    private String langCd;
    private String langNm;
    private String joinDt;
}
