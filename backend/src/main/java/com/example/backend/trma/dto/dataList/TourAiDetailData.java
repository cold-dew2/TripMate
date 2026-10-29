package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class TourAiDetailData {
    private String operatingHours;
    private String closedDays;
    private String admissionFeeIsFree;
    private String admissionFeeDetails;
    private String websiteUrl;
    private String parkingAvailable;
    private String parkingFeeInfo;
    private String lastUpdatedNote;
    // TourAPI 기반 이용정보에서 추가된 항목 (API가 주지 않으면 null)
    private String parkingInfo;   // 주차 시설 안내 원문 (예: "가능 (승용차 240대)")
    private String infoCenter;    // 문의 및 안내
    private String tel;           // 대표 전화
    private String source;        // TOURAPI / TOURAPI_TRANSLATED / TOURAPI_KO / NONE (전체 요약)
    // AI 정보 조사가 백그라운드에서 진행 중이면 true (화면은 잠시 뒤 다시 조회)
    private boolean researchPending;
    // 항목별 출처/번역 상태. key: operatingHours, closedDays, admissionFeeDetails, parkingInfo,
    // parkingFeeInfo, infoCenter, tel, websiteUrl (값이 있는 항목만)
    private java.util.Map<String, FieldInfo> fields = new java.util.LinkedHashMap<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FieldInfo {
        private String origin;       // TOURAPI(공식 원본) / AI_RESEARCH(AI가 출처 확인 후 보완)
        private String translation;  // NONE(원문 언어) / OFFICIAL(해당 언어 TourAPI) / MANUAL / TOURAPI / LEGACY / AI / KO_FALLBACK(번역 없어 한국어)
        private String sourceUrl;    // AI_RESEARCH일 때 확인한 출처
        private String sourceTitle;
        private String checkedAt;    // AI_RESEARCH 확인 시각 (ISO-8601)
    }
}
