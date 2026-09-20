package com.example.backend.trma.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class AiScheduleExistingItem {
    private int day;
    private String time;
    private String tourId;
}
