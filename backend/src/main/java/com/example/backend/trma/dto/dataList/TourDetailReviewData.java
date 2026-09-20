package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class TourDetailReviewData {
    private String reviewId;
    private String moimTitle;
    private String moimStartDt;
    private String moimEndDt;
    private String reviewTitle;
    private String reviewContent;
    private int reviewScore;
    private String imgUrls;
    private String creatDt;
    private String userNm;
    // 번역 캐시(응답에는 그대로 노출되지만, 화면에서는 reviewTitle/reviewContent만 씀)
    private String reviewTitleEn;
    private String reviewTitleJa;
    private String reviewContentEn;
    private String reviewContentJa;
}
