package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class AiScheduleItemData {
    private int day;
    private String time;
    private String tourId;
    private String tourNm;
    private String firstImage;
    private String roadAddr;
}
