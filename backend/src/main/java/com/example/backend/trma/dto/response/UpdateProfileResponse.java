package com.example.backend.trma.dto.response;

import com.example.backend.trma.dto.dataList.MyProfileData;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdateProfileResponse {
    private boolean success;
    private int status;
    private String code;
    private String message;
    private String path;
    private String token;

    private MyProfileData data;
}
