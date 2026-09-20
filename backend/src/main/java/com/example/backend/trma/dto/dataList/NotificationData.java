package com.example.backend.trma.dto.dataList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class NotificationData {
    private long notiId;
    private String typeCd;
    private String title;
    private String content;
    // 알림 문구를 화면 언어에 맞게 조립하기 위한 값(타입별로 의미가 다름).
    // APPLY: param1=신청자 이름, param2=모임 제목 / COMPLETE: param1=모임 제목
    private String param1;
    private String param2;
    private String linkUrl;
    private String isRead;
    private String createDt;
}
