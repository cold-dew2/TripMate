package com.example.backend.trma.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

// 새 알림(모임 신청/채팅/여행 완료)이 저장될 때마다 해당 사용자에게 실시간으로
// "새 알림이 있다"는 신호만 보낸다. 클라이언트는 이 신호를 받으면 알림 개수를
// 다시 조회해 갱신한다(내용 자체를 실어보내지 않아 단순하고 안전하다).
// 이 푸시는 부가 기능이므로, 실패하더라도 모임 신청/채팅 전송 등 원래 요청은
// 절대 실패시키면 안 된다 — 그래서 예외를 여기서 잡아 로그만 남긴다.
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPushService {

    private final SimpMessagingTemplate broker;

    public void pushToUser(String userId) {
        if (userId == null || userId.isBlank()) return;

        try {
            broker.convertAndSend("/topic/notifications/" + userId, "NEW");
        } catch (Exception e) {
            log.warn("실시간 알림 푸시에 실패했습니다. userId={}", userId, e);
        }
    }
}
