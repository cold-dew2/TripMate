package com.example.backend.trma.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
@Setter
public class TransportRecommendRequest {
    private List<TransportStopInput> items;

    @Getter
    @NoArgsConstructor
    @Setter
    public static class TransportStopInput {
        private int day;
        private String time;
        private String tourId;
        private String tourNm;
        private String roadAddr;
    }
}
