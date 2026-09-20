package com.example.backend.trma.dto.dataList;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MyTodayScheduleMoimData {
    private String moimId;
    private String moimTitle;
    private List<MyTodayScheduleItemData> items;
}
