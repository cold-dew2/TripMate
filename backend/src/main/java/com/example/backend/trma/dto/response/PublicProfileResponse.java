package com.example.backend.trma.dto.response;

import com.example.backend.trma.dto.dataList.MyMoimData;
import com.example.backend.trma.dto.dataList.PublicProfileData;
import com.example.backend.trma.dto.dataList.UserReviewData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PublicProfileResponse {
    private boolean success;
    private int status;
    private String code;
    private String message;
    private String path;
    private String token;

    private PublicProfileData data;
    private List<MyMoimData> moims;
    private List<UserReviewData> reviews;
}
