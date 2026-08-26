package com.example.backend.trma.dto.response;

import com.example.backend.trma.dto.dataList.TourAiDetailData;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TourAiDetailResponse {
    private boolean success;
    private int status;
    private String code;
    private String message;
    private String path;
    private String token;

    private TourAiDetailData data;
}
