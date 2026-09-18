package com.example.backend.trma.service;

import com.example.backend.trma.dto.response.MarkNotificationReadResponse;
import com.example.backend.trma.dto.response.NotificationsResponse;
import com.example.backend.trma.dto.response.UnreadCountResponse;

public interface NotificationService {
    //알림 목록 조회(여행 완료 알림 지연 생성 포함)
    NotificationsResponse notifications(String userId);

    //안읽은 알림 수 조회
    UnreadCountResponse unreadCount(String userId);

    //알림 읽음 처리
    MarkNotificationReadResponse markRead(long notiId, String userId);
}
