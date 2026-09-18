package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.ChatMessageData;
import com.example.backend.trma.dto.dataList.ChatRoomData;
import com.example.backend.trma.dto.dataList.NewChatMessage;
import com.example.backend.trma.dto.request.SendChatMessageRequest;
import com.example.backend.trma.dto.response.ChatMessagesResponse;
import com.example.backend.trma.dto.response.ChatRoomsResponse;
import com.example.backend.trma.dto.response.SendChatMessageResponse;
import com.example.backend.trma.dto.response.UnreadCountResponse;
import com.example.backend.trma.mapper.ChatMapper;
import com.example.backend.trma.mapper.NotificationMapper;
import com.example.backend.trma.service.ChatService;
import com.example.backend.trma.service.NotificationPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatMapper chatMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationPushService notificationPushService;
    private final SimpMessagingTemplate broker;

    //내 채팅방 목록 조회
    @Override
    public ChatRoomsResponse chatRooms(String userId) {

        try {
            List<ChatRoomData> rooms = chatMapper.chatRooms(userId);

            return new ChatRoomsResponse(
                    true,
                    200,
                    "SUCCESS",
                    "채팅방 목록을 정상적으로 조회했습니다.",
                    "/chat/rooms",
                    "",
                    rooms
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new ChatRoomsResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/chat/rooms",
                    "",
                    null
            );
        }
    }

    //채팅 메시지 목록 조회(조회 시 자동 입장 처리)
    @Override
    public ChatMessagesResponse chatMessages(String roomId, String userId) {

        try {
            chatMapper.insertRoomIfNotExists(roomId, roomId, userId);
            chatMapper.insertMemberIfNotExists(roomId, userId);

            List<ChatMessageData> messages = chatMapper.chatMessages(roomId);

            chatMapper.updateLastRead(roomId, userId);

            return new ChatMessagesResponse(
                    true,
                    200,
                    "SUCCESS",
                    "채팅 메시지를 정상적으로 조회했습니다.",
                    "/chat/rooms/" + roomId + "/messages",
                    "",
                    messages
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new ChatMessagesResponse(
                    false,
                    500,
                    "FAIL",
                    "조회 중 오류가 발생했습니다.",
                    "/chat/rooms/" + roomId + "/messages",
                    "",
                    null
            );
        }
    }

    //채팅 메시지 등록
    @Override
    public SendChatMessageResponse sendMessage(String roomId, SendChatMessageRequest request, String userId) {

        try {
            String title = request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle() : roomId;
            chatMapper.insertRoomIfNotExists(roomId, title, userId);
            chatMapper.insertMemberIfNotExists(roomId, userId);

            NewChatMessage newMessage = new NewChatMessage();
            newMessage.setRoomId(roomId);
            newMessage.setSenderId(userId);
            newMessage.setContent(request.getContent());
            chatMapper.insertMessage(newMessage);

            ChatMessageData saved = chatMapper.messageDetail(newMessage.getMessageId());
            broker.convertAndSend("/topic/chat/" + roomId, saved);
            // 알림 등록/실시간 푸시는 부가 기능이라 여기서 실패해도 메시지 전송 자체는
            // 이미 완료된 것으로 처리해야 하므로, 별도로 감싸서 전송 성공 여부에 영향을 주지 않게 한다.
            try {
                notificationMapper.insertChatNotifications(roomId, userId, saved.getSenderName(), request.getContent());
                for (String memberId : chatMapper.roomMemberIds(roomId)) {
                    if (!memberId.equals(userId)) {
                        notificationPushService.pushToUser(memberId);
                    }
                }
            } catch (Exception e) {
                log.warn("채팅 알림 등록/푸시에 실패했습니다. roomId={}", roomId, e);
            }

            return new SendChatMessageResponse(
                    true,
                    200,
                    "SUCCESS",
                    "메시지를 전송했습니다.",
                    "/chat/rooms/" + roomId + "/messages",
                    "",
                    saved
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new SendChatMessageResponse(
                    false,
                    500,
                    "FAIL",
                    "메시지 전송 중 오류가 발생했습니다.",
                    "/chat/rooms/" + roomId + "/messages",
                    "",
                    null
            );
        }
    }

    //전체 안읽은 메시지 수 조회
    @Override
    public UnreadCountResponse unreadTotal(String userId) {

        try {
            int total = chatMapper.unreadTotal(userId);

            return new UnreadCountResponse(
                    true,
                    200,
                    "SUCCESS",
                    "안읽은 메시지 수를 정상적으로 조회했습니다.",
                    "/chat/unreadCount",
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
                    "/chat/unreadCount",
                    "",
                    0
            );
        }
    }
}
