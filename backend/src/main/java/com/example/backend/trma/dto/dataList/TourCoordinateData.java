package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 교통편 추천(실시간 카카오모빌리티 길찾기)에 좌표가 필요해 관광지ID로 위도/경도만
// 가볍게 조회하는 용도.
@Getter
@NoArgsConstructor
@Setter
public class TourCoordinateData {
    private String tourId;
    private Double latitude;
    private Double longitude;
}
