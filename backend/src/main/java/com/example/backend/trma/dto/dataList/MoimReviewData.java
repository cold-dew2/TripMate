package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class MoimReviewData {
    private String userNm;
    private int reviewScore;
    private String reviewContent;
    private String imgUrls;
    private String createDt;
}
