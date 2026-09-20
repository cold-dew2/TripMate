package com.example.backend.trma.mapper;

import com.example.backend.trma.dto.dataList.NotificationData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {
    //모임 신청 알림 등록(모임장에게)
    int insertApplyNotification(@Param("moimId") String moimId,
                                @Param("applicantUserId") String applicantUserId);

    //채팅 알림 등록(발신자 제외 멤버 전체)
    int insertChatNotifications(@Param("roomId") String roomId,
                                @Param("senderId") String senderId,
                                @Param("senderName") String senderName,
                                @Param("content") String content);

    //여행 완료 알림 등록(지연 생성)
    int insertCompleteNotificationsIfNeeded(String userId);

    //알림 목록 조회
    List<NotificationData> notifications(String userId);

    //안읽은 알림 수
    int unreadNotificationCount(String userId);

    //알림 읽음 처리
    int markNotificationRead(@Param("notiId") long notiId, @Param("userId") String userId);

    //채팅방에 들어왔을 때, 그 방의 채팅 알림을 모두 읽음 처리
    int markChatNotificationsRead(@Param("roomId") String roomId, @Param("userId") String userId);

    //모임 관리(신청자 목록)에 들어왔을 때, 그 모임의 가입 신청 알림을 모두 읽음 처리
    int markApplyNotificationsRead(@Param("moimId") String moimId, @Param("userId") String userId);
}
