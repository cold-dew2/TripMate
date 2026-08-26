package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class TourAiDetailData {
    private String operatingHours;
    private String closedDays;
    private String admissionFeeIsFree;
    private String admissionFeeDetails;
    private String websiteUrl;
    private String parkingAvailable;
    private String parkingFeeInfo;
    private String lastUpdatedNote;
}
