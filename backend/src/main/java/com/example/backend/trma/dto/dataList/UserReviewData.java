package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class UserReviewData {
    private String reviewId;
    private String reviewTitle;
    private String reviewContent;
    private String reviewContentEn;
    private String reviewContentJa;
    private int reviewScore;
    private String userNm;
    private String profileImgUrl;
    private String imgUrls;
    private String createDt;
}
