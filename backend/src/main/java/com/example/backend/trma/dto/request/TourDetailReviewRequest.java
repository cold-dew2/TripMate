package com.example.backend.trma.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class TourDetailReviewRequest {
    private String tourId;
    private int page;
    private int offset;
    private String lang;
}
