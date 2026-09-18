package com.example.backend.trma.controller;

import com.example.backend.trma.dto.response.MarkNotificationReadResponse;
import com.example.backend.trma.dto.response.NotificationsResponse;
import com.example.backend.trma.dto.response.UnreadCountResponse;
import com.example.backend.trma.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    //알림 목록 조회
    @GetMapping
    public NotificationsResponse notifications(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new NotificationsResponse(false, 500, "NEED_LOGIN", "로그인이 필요합니다.", "/notifications", "", null);
        }

        return notificationService.notifications(authentication.getName());
    }

    //안읽은 알림 수 조회
    @GetMapping("/unreadCount")
    public UnreadCountResponse unreadCount(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new UnreadCountResponse(false, 500, "NEED_LOGIN", "로그인이 필요합니다.", "/notifications/unreadCount", "", 0);
        }

        return notificationService.unreadCount(authentication.getName());
    }

    //알림 읽음 처리
    @PutMapping("/{notiId}/read")
    public MarkNotificationReadResponse markRead(@PathVariable long notiId, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return new MarkNotificationReadResponse(false, 500, "NEED_LOGIN", "로그인이 필요합니다.", "/notifications/" + notiId + "/read", "");
        }

        return notificationService.markRead(notiId, authentication.getName());
    }
}
