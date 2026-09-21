package com.example.backend.trma.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class TourSearchRequest {
    private String keyword;
    private String cateCd;
    // 지역 탭 전용 필터. keyword로 지역명을 검색하면 "세종대로"(서울 소재 도로명), "세종대왕"
    // (다른 지역 관광지 설명문) 같은 우연한 일치까지 걸려 엉뚱한 지역이 섞여 나왔다.
    // 이 필드는 도로명주소가 그 지역명으로 "시작하는" 것만 정확히 걸러낸다.
    // 값이 "OTHER"면 등록된 지역(세종/서울/부산/제주) 어디에도 속하지 않는 관광지만 걸러낸다.
    private String region;
    private int page;
    private int offset;
    private String lang;
}
