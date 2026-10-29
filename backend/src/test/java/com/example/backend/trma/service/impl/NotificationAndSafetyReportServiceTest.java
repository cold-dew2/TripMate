package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.request.CreateSafetyReportRequest;
import com.example.backend.trma.mapper.NotificationMapper;
import com.example.backend.trma.mapper.SafetyReportMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationAndSafetyReportServiceTest {

    @Mock NotificationMapper notificationMapper;
    @Mock SafetyReportMapper safetyReportMapper;

    @Nested
    @DisplayName("[알림] NotificationService")
    class Notification {
        NotificationServiceImpl service() { return new NotificationServiceImpl(notificationMapper); }

        @Test
        @DisplayName("목록 조회 전에 여행 완료 알림을 먼저 생성한다")
        void list_createsCompleteNotificationsFirst() {
            when(notificationMapper.notifications("u")).thenReturn(List.of());

            assertThat(service().notifications("u").isSuccess()).isTrue();
            InOrder order = inOrder(notificationMapper);
            order.verify(notificationMapper).insertCompleteNotificationsIfNeeded("u");
            order.verify(notificationMapper).notifications("u");
        }

        @Test
        @DisplayName("안읽은 알림 수를 돌려준다")
        void unreadCount() {
            when(notificationMapper.unreadNotificationCount("u")).thenReturn(3);

            assertThat(service().unreadCount("u").getData()).isEqualTo(3);
        }

        @Test
        @DisplayName("읽음 처리는 본인 알림(userId 조건)으로만 요청한다")
        void markRead() {
            assertThat(service().markRead(10L, "u").isSuccess()).isTrue();
            verify(notificationMapper).markNotificationRead(10L, "u");
        }

        @Test
        @DisplayName("DB 오류 시 500과 0건으로 응답한다")
        void dbError() {
            doThrow(new RuntimeException()).when(notificationMapper).insertCompleteNotificationsIfNeeded("u");

            assertThat(service().unreadCount("u").getStatus()).isEqualTo(500);
            assertThat(service().unreadCount("u").getData()).isZero();
        }
    }

    @Nested
    @DisplayName("[안심신고] SafetyReportService")
    class SafetyReport {
        @Test
        @DisplayName("신고를 신고자 아이디와 함께 저장한다")
        void create() {
            CreateSafetyReportRequest request = new CreateSafetyReportRequest();

            assertThat(new SafetyReportServiceImpl(safetyReportMapper).createSafetyReport(request, "u").isSuccess()).isTrue();
            verify(safetyReportMapper).insertSafetyReport(request, "u");
        }

        @Test
        @DisplayName("저장 실패 시 500")
        void create_fail() {
            doThrow(new RuntimeException()).when(safetyReportMapper).insertSafetyReport(any(), any());

            assertThat(new SafetyReportServiceImpl(safetyReportMapper).createSafetyReport(new CreateSafetyReportRequest(), "u").getStatus()).isEqualTo(500);
        }
    }
}
