package com.example.backend.trma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FindIdResponse {
    private boolean success;
    private int status;
    private String code;
    private String message;
    private String path;
    private List<String> data;
}
