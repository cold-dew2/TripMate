package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.ChatMessageData;
import com.example.backend.trma.dto.dataList.ChatRoomData;
import com.example.backend.trma.dto.dataList.ChatUnreadDeltaData;
import com.example.backend.trma.dto.dataList.NewChatMessage;
import com.example.backend.trma.dto.request.SendChatMessageRequest;
import com.example.backend.trma.dto.response.ChatMessagesResponse;
import com.example.backend.trma.dto.response.ChatRoomsResponse;
import com.example.backend.trma.dto.response.SendChatMessageResponse;
import com.example.backend.trma.dto.response.LeaveChatRoomResponse;
import com.example.backend.trma.dto.response.MarkChatReadResponse;
import com.example.backend.trma.dto.response.UnreadCountResponse;
import com.example.backend.trma.mapper.ChatMapper;
import com.example.backend.trma.mapper.MoimListMapper;
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
    private final MoimListMapper moimListMapper;
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
            String resolvedTitle = resolveRoomTitle(roomId, null);
            chatMapper.insertRoomIfNotExists(roomId, resolvedTitle, userId);
            chatMapper.fixRoomTitleIfStale(roomId, resolvedTitle);
            chatMapper.insertMemberIfNotExists(roomId, userId);

            // 메시지를 읽기 전에 먼저 읽음 처리를 해야, 지금 막 보는 메시지들의
            // "안읽은 사람 수"에 자기 자신이 포함되는 걸 막을 수 있다. 다른 참여자들에게는
            // 전체 메시지를 다시 보내는 대신 바뀐 안읽음 수만 가볍게 실어 보낸다.
            markReadAndBroadcastDelta(roomId, userId);

            List<ChatMessageData> messages = chatMapper.chatMessages(roomId);
            int memberCount = chatMapper.roomMemberIds(roomId).size();
            String myState = chatMapper.chatMemberState(roomId, userId);

            return new ChatMessagesResponse(
                    true,
                    200,
                    "SUCCESS",
                    "채팅 메시지를 정상적으로 조회했습니다.",
                    "/chat/rooms/" + roomId + "/messages",
                    "",
                    messages,
                    memberCount,
                    myState
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
                    null,
                    0,
                    null
            );
        }
    }

    //채팅 메시지 등록
    @Override
    public SendChatMessageResponse sendMessage(String roomId, SendChatMessageRequest request, String userId) {

        try {
            String title = resolveRoomTitle(roomId, request.getTitle());
            chatMapper.insertRoomIfNotExists(roomId, title, userId);
            chatMapper.insertMemberIfNotExists(roomId, userId);

            if ("K".equals(chatMapper.chatMemberState(roomId, userId))) {
                return new SendChatMessageResponse(
                        false,
                        403,
                        "KICKED",
                        "이 채팅방에서 추방되어 메시지를 보낼 수 없습니다.",
                        "/chat/rooms/" + roomId + "/messages",
                        "",
                        null
                );
            }

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

    //채팅방 나가기(소모임 채팅방이면 모임 탈퇴까지 함께 처리)
    @Override
    public LeaveChatRoomResponse leaveChatRoom(String roomId, String userId) {

        try {
            String moimId = extractMoimId(roomId);

            // 모임장이 나가버리면 모임 자체가 주인 없이 남기 때문에, 이 흐름으로는
            // 나갈 수 없게 막는다.
            if (moimId != null && userId.equals(moimListMapper.moimHostUserId(moimId))) {
                return new LeaveChatRoomResponse(
                        false,
                        400,
                        "HOST_CANNOT_LEAVE",
                        "모임장은 채팅방을 나갈 수 없습니다.",
                        "/chat/rooms/" + roomId
                );
            }

            chatMapper.deleteChatMember(roomId, userId);
            if (moimId != null) {
                moimListMapper.deleteMoimMember(moimId, userId);
            }

            return new LeaveChatRoomResponse(
                    true,
                    200,
                    "SUCCESS",
                    "채팅방에서 나갔습니다.",
                    "/chat/rooms/" + roomId
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new LeaveChatRoomResponse(
                    false,
                    500,
                    "FAIL",
                    "처리 중 오류가 발생했습니다.",
                    "/chat/rooms/" + roomId
            );
        }
    }

    //채팅방 읽음 처리만 가볍게(전체 메시지 재조회 없이 안읽음 수 델타만 반환/방송)
    @Override
    public MarkChatReadResponse markRead(String roomId, String userId) {

        try {
            List<ChatUnreadDeltaData> delta = markReadAndBroadcastDelta(roomId, userId);

            return new MarkChatReadResponse(
                    true,
                    200,
                    "SUCCESS",
                    "읽음 처리했습니다.",
                    "/chat/rooms/" + roomId + "/read",
                    delta
            );
        } catch (Exception e) {
            log.error("처리 중 오류가 발생했습니다.", e);
            return new MarkChatReadResponse(
                    false,
                    500,
                    "FAIL",
                    "처리 중 오류가 발생했습니다.",
                    "/chat/rooms/" + roomId + "/read",
                    null
            );
        }
    }

    // 방을 읽음 처리하고, 그로 인해 안읽음 수가 바뀔 수 있는 메시지들의 최신 카운트를
    // 계산해서 다른 참여자들에게 방송한다. 전체 메시지(본문/발신자 등)를 다시 보내지
    // 않고 "메시지ID + 새 카운트" 쌍만 보내는 게 핵심 — chatMessages(최초 조회)와
    // markRead(이후 갱신) 양쪽에서 공유한다.
    private List<ChatUnreadDeltaData> markReadAndBroadcastDelta(String roomId, String userId) {
        chatMapper.updateLastRead(roomId, userId);
        // 알림 목록을 거치지 않고 채팅방에 바로 들어온 경우에도 그 방의 채팅 알림을
        // 읽음 처리해서, 홈 화면 알림 배지가 계속 남아있지 않도록 한다.
        notificationMapper.markChatNotificationsRead(roomId, userId);

        List<ChatUnreadDeltaData> delta = chatMapper.unreadCountsForRoom(roomId, userId);
        // 지금 이 방을 열어둔 다른 참여자들도(그리고 나 자신도) "안읽은 사람 수" 배지가
        // 실시간으로 줄어드는 걸 보도록, 새 메시지 토픽과는 별개의 채널로 보낸다.
        broker.convertAndSend("/topic/chat/" + roomId + "/read", delta);
        return delta;
    }

    private String extractMoimId(String roomId) {
        return (roomId != null && roomId.startsWith("moim-")) ? roomId.substring("moim-".length()) : null;
    }

    // 프론트가 방 제목을 안 넘긴 채(예: 채팅 목록에서 바로 진입, 알림 클릭 등) 방을 처음
    // 만들게 되면 roomId 문자열("moim-XXXX")이 그대로 제목으로 저장돼버리던 문제가 있었다.
    // roomId가 "moim-{moimId}" 형식인 소모임 채팅방은 실제 모임 이름을 기본 제목으로 쓴다.
    private String resolveRoomTitle(String roomId, String requestedTitle) {
        if (requestedTitle != null && !requestedTitle.isBlank()) {
            return requestedTitle;
        }

        if (roomId != null && roomId.startsWith("moim-")) {
            String moimId = roomId.substring("moim-".length());
            String moimTitle = chatMapper.moimTitle(moimId);
            if (moimTitle != null && !moimTitle.isBlank()) {
                return moimTitle;
            }
        }

        return roomId;
    }
}
