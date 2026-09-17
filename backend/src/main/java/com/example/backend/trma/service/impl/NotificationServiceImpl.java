package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.NotificationData;
import com.example.backend.trma.dto.response.MarkNotificationReadResponse;
import com.example.backend.trma.dto.response.NotificationsResponse;
import com.example.backend.trma.dto.response.UnreadCountResponse;
import com.example.backend.trma.mapper.NotificationMapper;
import com.example.backend.trma.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    //알림 목록 조회(여행 완료 알림 지연 생성 포함)
    @Override
    public NotificationsResponse notifications(String userId) {

        try {
            notificationMapper.insertCompleteNotificationsIfNeeded(userId);

            List<NotificationData> notifications = notificationMapper.notifications(userId);

            return new NotificationsResponse(
                    true,
                    200,
                    "SUCCESS",
                    "알림 목록을 정상적으로 조회했습니다.",
                    "/notifications",
                    "",
                    notifications
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new NotificationsResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/notifications",
                    "",
                    null
            );
        }
    }

    //안읽은 알림 수 조회
    @Override
    public UnreadCountResponse unreadCount(String userId) {

        try {
            notificationMapper.insertCompleteNotificationsIfNeeded(userId);

            int total = notificationMapper.unreadNotificationCount(userId);

            return new UnreadCountResponse(
                    true,
                    200,
                    "SUCCESS",
                    "안읽은 알림 수를 정상적으로 조회했습니다.",
                    "/notifications/unreadCount",
                    "",
                    total
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new UnreadCountResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/notifications/unreadCount",
                    "",
                    0
            );
        }
    }

    //알림 읽음 처리
    @Override
    public MarkNotificationReadResponse markRead(long notiId, String userId) {

        try {
            notificationMapper.markNotificationRead(notiId, userId);

            return new MarkNotificationReadResponse(
                    true,
                    200,
                    "SUCCESS",
                    "알림을 읽음 처리했습니다.",
                    "/notifications/" + notiId + "/read",
                    ""
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new MarkNotificationReadResponse(
                    false,
                    500,
                    "FAIL",
                    "처리 중 오류가 발생했습니다.",
                    "/notifications/" + notiId + "/read",
                    ""
            );
        }
    }
}
