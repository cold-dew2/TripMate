package com.example.backend.trma.dto.response;

import com.example.backend.trma.dto.dataList.TransportLegData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TransportRecommendResponse {
    private boolean success;
    private int status;
    private String code;
    private String message;
    private String path;
    private String token;

    private List<TransportLegData> data;
}
