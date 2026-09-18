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
    private String reviewTitle;
    private String reviewContent;
    private int reviewScore;
    private List<String> imageUrls;
}
