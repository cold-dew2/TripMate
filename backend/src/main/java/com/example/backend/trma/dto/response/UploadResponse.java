package com.example.backend.trma.dto.response;

import com.example.backend.trma.dto.dataList.UploadData;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UploadResponse {
    private boolean success;
    private int status;
    private String code;
    private String message;
    private String path;
    private String token;

    private UploadData data;
}
