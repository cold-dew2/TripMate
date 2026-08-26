package com.example.backend.trma.dto.response;

import com.example.backend.trma.dto.dataList.TourDetailData;
import com.example.backend.trma.dto.dataList.TourSearchData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TourDetailResponse {
    private boolean success;
    private int status;
    private String code;
    private String message;
    private String path;
    private String token;

    private TourDetailData data;
}
