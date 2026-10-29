package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// TourAPI detailIntro2 / detailCommon2에서 뽑은 이용 정보. API가 주지 않은 항목은 null로 둔다.
@Getter
@Setter
@NoArgsConstructor
public class TourIntroData {
    private String tourId;
    private String langCd;
    private String contentId;
    private String contentTypeId;
    private String useTime;
    private String restDate;
    private String useFee;
    private String parking;
    private String parkingFee;
    private String infoCenter;
    private String homepage;
    private String tel;
    private String introRaw;
    private String commonRaw;
    private String fetchStatus;
    private LocalDateTime fetchDt;
}
