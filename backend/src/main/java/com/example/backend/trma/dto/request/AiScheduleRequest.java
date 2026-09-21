package com.example.backend.trma.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
@Setter
public class AiScheduleRequest {
    private String cateCd;
    private String keyword;
    // 지역 탭과 동일한 전용 필터(도로명주소 접두사 매칭). keyword(자유 검색어)와
    // 달리 "세종대로"/"세종대왕" 같은 우연한 일치 없이 정확히 그 지역만 걸러낸다.
    private String region;
    private int dayCount;
    private String cateNms;
    private Integer maxMember;
    private String moimStartDt;
    private String moimEndDt;
    // 사용자가 이미 화면에서 직접 추가해둔 일정. 있으면 이 일정은 그대로 두고, 빈
    // 시간대만 채우도록 추천한다.
    private List<AiScheduleExistingItem> existingItems;
    private String lang;
}
