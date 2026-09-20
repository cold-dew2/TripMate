package com.example.backend.trma.dto.dataList;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecentReviewData {
    private String reviewId;
    private String reviewerName;
    private String reviewerProfileImgUrl;
    private int reviewScore;
    private String reviewContent;
    private String imgUrls;
    private String createDt;
}
