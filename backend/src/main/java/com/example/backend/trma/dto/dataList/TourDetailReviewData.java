package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class TourDetailReviewData {
    private String moimTitle;
    private String moimStartDt;
    private String moimEndDt;
    private String reviewTitle;
    private String reviewContent;
    private int reviewScore;
    private String creatDt;
    private String userNm;
}
