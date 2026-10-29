package com.example.backend.config;

import com.example.backend.trma.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("[채팅/알림] WebSocket 구독 권한")
class StompSubscriptionGuardTest {

    @Mock ChatService chatService;
    @Mock ObjectProvider<ChatService> provider;
    StompSubscriptionGuard guard;

    @BeforeEach
    void setUp() {
        when(provider.getObject()).thenReturn(chatService);
        guard = new StompSubscriptionGuard(provider);
        when(chatService.canReadRoom("moim-M1", "member@t.com")).thenReturn(true);
    }

    Message<?> frame(StompCommand command, String destination, String userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        if (userId != null) accessor.setUser(new UsernamePasswordAuthenticationToken(userId, null));
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("멤버는 채팅방의 메시지/읽음/번역 채널을 모두 구독할 수 있다")
    void memberAllowed() {
        assertThat(guard.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat/moim-M1", "member@t.com"), null)).isNotNull();
        assertThat(guard.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat/moim-M1/read", "member@t.com"), null)).isNotNull();
        assertThat(guard.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat/moim-M1/translated", "member@t.com"), null)).isNotNull();
    }

    @Test
    @DisplayName("멤버가 아니거나 로그인하지 않았으면 채팅 구독을 버린다")
    void nonMemberDropped() {
        assertThat(guard.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat/moim-M1", "stranger@t.com"), null)).isNull();
        assertThat(guard.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat/moim-M1", null), null)).isNull();
        verify(chatService, never()).canReadRoom("moim-M1", null);
    }

    @Test
    @DisplayName("알림 채널은 본인 것만 구독할 수 있다")
    void notificationsOwnOnly() {
        assertThat(guard.preSend(frame(StompCommand.SUBSCRIBE, "/topic/notifications/me@t.com", "me@t.com"), null)).isNotNull();
        assertThat(guard.preSend(frame(StompCommand.SUBSCRIBE, "/topic/notifications/other@t.com", "me@t.com"), null)).isNull();
        assertThat(guard.preSend(frame(StompCommand.SUBSCRIBE, "/topic/notifications/me@t.com", null), null)).isNull();
    }

    @Test
    @DisplayName("구독이 아닌 프레임(CONNECT/SEND 등)은 그대로 통과시킨다")
    void nonSubscribePasses() {
        assertThat(guard.preSend(frame(StompCommand.CONNECT, null, null), null)).isNotNull();
    }
}
