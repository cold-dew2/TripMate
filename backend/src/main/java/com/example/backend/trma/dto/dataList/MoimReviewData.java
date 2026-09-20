package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class MoimReviewData {
    private String reviewId;
    private String userNm;
    private int reviewScore;
    private String reviewContent;
    private String imgUrls;
    private String createDt;
    // 번역 캐시(응답에는 그대로 노출되지만, 화면에서는 reviewContent만 씀)
    private String reviewContentEn;
    private String reviewContentJa;
}
