package com.example.backend.trma.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class AiScheduleRequest {
    private String cateCd;
    private String keyword;
    private int dayCount;
}
