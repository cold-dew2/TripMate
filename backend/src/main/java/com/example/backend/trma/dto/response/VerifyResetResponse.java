package com.example.backend.trma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VerifyResetResponse {
    private boolean success;
    private int status;
    private String code;
    private String message;
    private String path;
}
