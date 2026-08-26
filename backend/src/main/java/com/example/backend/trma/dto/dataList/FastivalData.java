package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class FastivalData {
    private String contentId;
    private String contentTypeId;
    private String title;
    private String createdTime;
    private String modifiedTime;
    private String eventStartDate;
    private String eventEndDate;
    private String firstImage;
    private String firstImage2;
    private String cpyrhtDivCd;
    private String addr1;
    private String addr2;
    private String zipcode;
    private String mapX;
    private String mapY;
    private String mLevel;
    private String lDongRegnCd;
    private String lDongSignguCd;
    private String lclsSystm1;
    private String lclsSystm2;
    private String lclsSystm3;
    private String progressType;
    private String festivalType;
}
