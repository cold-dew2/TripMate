package com.example.backend.trma.dto.response;

import com.example.backend.trma.dto.dataList.TourDetailData;
import com.example.backend.trma.dto.dataList.UserDetailData;
import com.example.backend.trma.dto.dataList.UserReviewData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserDetailResponse {
    private boolean success;
    private int status;
    private String code;
    private String message;
    private String path;
    private String token;

    private UserDetailData data;
    private List<UserReviewData> review;
}