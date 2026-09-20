package com.example.backend.trma.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class UserReviewRequest {
    private String userId;
    private int page;
    private int offset;
    // "latest"(최신순, 기본값) 또는 "rating"(별점순)
    private String sort;
}