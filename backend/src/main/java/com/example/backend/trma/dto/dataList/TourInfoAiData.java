package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// AI가 신뢰할 수 있는 출처에서 조사한 이용정보 한 항목 (TB_TRMA_TOUR_INFO_AI). TourAPI 원본과 별도 관리.
@Getter
@Setter
@NoArgsConstructor
public class TourInfoAiData {
    private String tourId;
    private String fieldNm;
    private String statusCd;     // FOUND / NOT_FOUND
    private String infoText;     // 한국어 값 (FOUND일 때만)
    private String sourceUrl;
    private String sourceTitle;
    private LocalDateTime checkedDt;
}
