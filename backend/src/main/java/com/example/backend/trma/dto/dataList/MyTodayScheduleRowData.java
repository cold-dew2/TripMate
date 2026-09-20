package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// myTodaySchedule 쿼리의 평평한(flat) 원본 행. 모임 하나에 오늘 일정이 여러 개면
// 같은 MOIM_ID로 여러 행이 나오므로, 서비스 계층에서 MOIM_ID 기준으로 묶어
// MyTodayScheduleMoimData 목록으로 변환한다.
@Getter
@NoArgsConstructor
@Setter
public class MyTodayScheduleRowData {
    private String moimId;
    private String moimTitle;
    private String time;
    private String tourId;
    private String placeName;
}
