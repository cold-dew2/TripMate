package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class UserReviewData {
    private String reviewTitle;
    private String reviewContent;
    private int reviewScore;
    private String userNm;
}
