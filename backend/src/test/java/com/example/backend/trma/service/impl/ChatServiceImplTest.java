package com.example.backend.trma.service.impl;

import com.example.backend.trma.dto.dataList.ChatMessageData;
import com.example.backend.trma.dto.dataList.ChatRoomData;
import com.example.backend.trma.dto.dataList.MoimJoinStatusData;
import com.example.backend.trma.dto.dataList.NewChatMessage;
import com.example.backend.trma.dto.request.SendChatMessageRequest;
import com.example.backend.trma.dto.response.SendChatMessageResponse;
import com.example.backend.trma.mapper.ChatMapper;
import com.example.backend.trma.mapper.MoimListMapper;
import com.example.backend.trma.mapper.NotificationMapper;
import com.example.backend.trma.service.NotificationPushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("[채팅] ChatService")
class ChatServiceImplTest {

    @Mock ChatMapper chatMapper;
    @Mock MoimListMapper moimListMapper;
    @Mock NotificationMapper notificationMapper;
    @Mock NotificationPushService notificationPushService;
    @Mock SimpMessagingTemplate broker;
    @Mock RestClient restClient;
    ChatServiceImpl service;

    static final String ROOM = "moim-M0000000001";
    static final String ME = "me@t.com";

    @BeforeEach
    void setUp() {
        service = new ChatServiceImpl(chatMapper, moimListMapper, notificationMapper, notificationPushService, broker, restClient);
        when(chatMapper.moimTitle("M0000000001")).thenReturn("세종 나들이");
        // 기본값: ME는 승인된 모임 멤버
        when(moimListMapper.moimJoinStatus("M0000000001", ME)).thenReturn(joinStatus("Y"));
    }

    static MoimJoinStatusData joinStatus(String stateCd) {
        MoimJoinStatusData status = new MoimJoinStatusData();
        status.setStateCd(stateCd);
        return status;
    }

    private SendChatMessageRequest message(String content) {
        SendChatMessageRequest request = new SendChatMessageRequest();
        request.setContent(content);
        return request;
    }

    @Test
    @DisplayName("메시지 전송: 저장 후 방 토픽으로 방송하고, 나를 뺀 멤버에게만 알림을 푸시한다")
    void send_success() {
        ChatMessageData saved = new ChatMessageData();
        saved.setMessageId("1");
        saved.setRoomId(ROOM);
        saved.setSenderName("나");
        saved.setContent("안녕하세요");
        when(chatMapper.messageDetail(anyLong())).thenReturn(saved);
        when(chatMapper.roomMemberIds(ROOM)).thenReturn(List.of(ME, "a@t.com", "b@t.com"));

        SendChatMessageResponse response = service.sendMessage(ROOM, message("안녕하세요"), ME);

        assertThat(response.isSuccess()).isTrue();
        verify(chatMapper).insertMessage(argThat((NewChatMessage m) -> ROOM.equals(m.getRoomId()) && ME.equals(m.getSenderId())));
        verify(broker).convertAndSend("/topic/chat/" + ROOM, saved);
        verify(notificationPushService).pushToUser("a@t.com");
        verify(notificationPushService).pushToUser("b@t.com");
        verify(notificationPushService, never()).pushToUser(ME);
    }

    @Test
    @DisplayName("방 제목을 안 넘기면 소모임 이름으로 방을 만든다")
    void send_resolvesRoomTitle() {
        when(chatMapper.messageDetail(anyLong())).thenReturn(new ChatMessageData());
        when(chatMapper.roomMemberIds(ROOM)).thenReturn(List.of());

        service.sendMessage(ROOM, message("hi"), ME);

        verify(chatMapper).insertRoomIfNotExists(ROOM, "세종 나들이", ME);
    }

    @Test
    @DisplayName("빈 메시지/공백 메시지는 400으로 거절한다")
    void send_empty() {
        assertThat(service.sendMessage(ROOM, message("   "), ME).getCode()).isEqualTo("EMPTY_CONTENT");
        assertThat(service.sendMessage(ROOM, message(null), ME).getCode()).isEqualTo("EMPTY_CONTENT");
        verify(chatMapper, never()).insertMessage(any());
    }

    @Test
    @DisplayName("추방(K)된 멤버는 메시지를 보낼 수 없다")
    void send_kicked() {
        when(chatMapper.chatMemberState(ROOM, ME)).thenReturn("K");

        assertThat(service.sendMessage(ROOM, message("hi"), ME).getStatus()).isEqualTo(403);
        verify(chatMapper, never()).insertMessage(any());
    }

    @Test
    @DisplayName("알림 저장이 실패해도 메시지 전송은 성공한다")
    void send_notificationFails() {
        when(chatMapper.messageDetail(anyLong())).thenReturn(new ChatMessageData());
        doThrow(new RuntimeException("noti fail")).when(notificationMapper).insertChatNotifications(any(), any(), any(), any());

        assertThat(service.sendMessage(ROOM, message("hi"), ME).isSuccess()).isTrue();
    }

    @Test
    @DisplayName("읽음 처리: 마지막 읽음 갱신, 채팅 알림 읽음, 안읽음 수 변경분 방송")
    void markRead() {
        assertThat(service.markRead(ROOM, ME).isSuccess()).isTrue();
        verify(chatMapper).updateLastRead(ROOM, ME);
        verify(notificationMapper).markChatNotificationsRead(ROOM, ME);
        verify(broker).convertAndSend(eq("/topic/chat/" + ROOM + "/read"), any(Object.class));
    }

    @Test
    @DisplayName("채팅방 나가기: 모임장은 나갈 수 없다")
    void leave_host() {
        when(moimListMapper.moimHostUserId("M0000000001")).thenReturn(ME);

        assertThat(service.leaveChatRoom(ROOM, ME).getCode()).isEqualTo("HOST_CANNOT_LEAVE");
        verify(chatMapper, never()).deleteChatMember(any(), any());
    }

    @Test
    @DisplayName("채팅방 나가기: 소모임 방이면 모임 탈퇴까지 처리한다")
    void leave_member() {
        when(moimListMapper.moimHostUserId("M0000000001")).thenReturn("host@t.com");

        assertThat(service.leaveChatRoom(ROOM, ME).isSuccess()).isTrue();
        verify(chatMapper).deleteChatMember(ROOM, ME);
        verify(moimListMapper).deleteMoimMember("M0000000001", ME);
    }

    // ── 채팅방 접근 권한 ──

    @Test
    @DisplayName("[권한] 모임 멤버가 아니면 대화 조회/전송/읽음 처리 모두 403이고, 멤버로 자동 등록되지 않는다")
    void nonMemberDenied() {
        String stranger = "stranger@t.com";

        assertThat(service.chatMessages(ROOM, stranger).getStatus()).isEqualTo(403);
        assertThat(service.sendMessage(ROOM, message("hi"), stranger).getStatus()).isEqualTo(403);
        assertThat(service.markRead(ROOM, stranger).getStatus()).isEqualTo(403);
        verify(chatMapper, never()).insertMemberIfNotExists(any(), eq(stranger));
        verify(chatMapper, never()).insertMessage(any());
        verify(chatMapper, never()).chatMessages(any());
    }

    @Test
    @DisplayName("[권한] 승인 대기(N) 중인 신청자는 채팅방에 들어갈 수 없다")
    void pendingApplicantDenied() {
        when(moimListMapper.moimJoinStatus("M0000000001", "wait@t.com")).thenReturn(joinStatus("N"));

        assertThat(service.canReadRoom(ROOM, "wait@t.com")).isFalse();
        assertThat(service.chatMessages(ROOM, "wait@t.com").getCode()).isEqualTo("NOT_MEMBER");
    }

    @Test
    @DisplayName("[권한] 모임장과 승인된 멤버는 대화를 볼 수 있다")
    void hostAndMemberAllowed() {
        when(moimListMapper.moimHostUserId("M0000000001")).thenReturn("host@t.com");
        when(chatMapper.chatMessages(ROOM)).thenReturn(List.of());

        assertThat(service.chatMessages(ROOM, "host@t.com").isSuccess()).isTrue();
        assertThat(service.chatMessages(ROOM, ME).isSuccess()).isTrue();
        verify(chatMapper).insertMemberIfNotExists(ROOM, ME);
    }

    @Test
    @DisplayName("[권한] 추방된 사람은 기록은 볼 수 있지만 메시지는 보낼 수 없다")
    void kickedCanReadButNotSend() {
        String kicked = "kicked@t.com";
        when(chatMapper.chatMemberState(ROOM, kicked)).thenReturn("K");
        when(chatMapper.chatMessages(ROOM)).thenReturn(List.of());

        assertThat(service.chatMessages(ROOM, kicked).isSuccess()).isTrue();
        assertThat(service.sendMessage(ROOM, message("hi"), kicked).getCode()).isEqualTo("NOT_MEMBER");
    }

    @Test
    @DisplayName("[권한] 소모임이 아닌 방은 기존 멤버만, 로그인 안 한 사용자는 어떤 방도 불가")
    void nonMoimRoomAndAnonymous() {
        when(chatMapper.roomMemberIds("dm-1")).thenReturn(List.of("a@t.com"));

        assertThat(service.canReadRoom("dm-1", "a@t.com")).isTrue();
        assertThat(service.canReadRoom("dm-1", "b@t.com")).isFalse();
        assertThat(service.canReadRoom(ROOM, null)).isFalse();
    }

    static ChatRoomData room(String roomId, int unread) {
        ChatRoomData r = new ChatRoomData();
        r.setRoomId(roomId);
        r.setTitle(roomId);
        r.setUnreadCount(unread);
        return r;
    }

    @Test
    @DisplayName("[권한] 채팅 목록/안읽음 합계에서 열 수 없는 방(예전 자동 입장 흔적)은 뺀다")
    void staleRoomsHidden() {
        // ME는 M0000000001의 승인 멤버, M0000000002에는 멤버가 아님
        when(chatMapper.chatRooms(ME)).thenReturn(List.of(room(ROOM, 3), room("moim-M0000000002", 5)));

        assertThat(service.chatRooms(ME, "ko").getData()).extracting(ChatRoomData::getRoomId).containsExactly(ROOM);
        assertThat(service.unreadTotal(ME).getData()).isEqualTo(3);
    }
}
