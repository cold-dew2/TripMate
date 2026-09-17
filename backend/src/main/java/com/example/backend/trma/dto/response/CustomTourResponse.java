package com.example.backend.trma.dto.response;

import com.example.backend.trma.dto.dataList.CustomTourData;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomTourResponse {
    private boolean success;
    private int status;
    private String code;
    private String message;
    private String path;
    private String token;

    private CustomTourData data;
}
