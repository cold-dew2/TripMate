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
    private int dayCount;
    private String cateNms;
    private Integer maxMember;
    private String moimStartDt;
    private String moimEndDt;
    // 사용자가 이미 화면에서 직접 추가해둔 일정. 있으면 이 일정은 그대로 두고, 빈
    // 시간대만 채우도록 추천한다.
    private List<AiScheduleExistingItem> existingItems;
}
