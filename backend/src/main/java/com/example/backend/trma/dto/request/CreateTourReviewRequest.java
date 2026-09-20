package com.example.backend.trma.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
@Setter
public class CreateTourReviewRequest {
    private String tourId;
    // 여행완료 후기등록 플로우에서 특정 소모임의 일정으로 방문한 관광지 후기를 남길 때만
    // 채워진다. 관광지 상세 화면에서 단독으로 남기는 후기는 null.
    private String moimId;
    private String reviewTitle;
    private String reviewContent;
    private int reviewScore;
    private List<String> imageUrls;
}
