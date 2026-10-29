package com.example.backend.config;

import com.example.backend.trma.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;

// 예전에는 STOMP 구독에 아무 확인이 없어서, 로그인하지 않은 사람도 /topic/chat/{roomId}를
// 구독하면 남의 소모임 대화를 실시간으로 받아볼 수 있었다. 구독(SUBSCRIBE) 시점에
// 핸드셰이크 때 JWT 쿠키로 인증된 사용자(Principal)를 기준으로 권한을 확인한다.
// 권한이 없으면 ERROR 프레임으로 연결을 끊지 않고 그 구독만 조용히 버린다. 연결을 끊으면
// 같은 연결의 다른 구독(알림 등)까지 함께 끊기고, 클라이언트가 재연결을 무한 반복하기 때문이다.
@Slf4j
@Component
@RequiredArgsConstructor
public class StompSubscriptionGuard implements ChannelInterceptor {

    private static final String CHAT_PREFIX = "/topic/chat/";
    private static final String NOTIFICATION_PREFIX = "/topic/notifications/";

    // ChatService는 메시지 브로커(SimpMessagingTemplate)에 의존하고, 브로커 설정은 이
    // 인터셉터를 등록하는 쪽이라 바로 주입하면 순환 참조가 생긴다. 사용할 때 꺼내 쓴다.
    private final ObjectProvider<ChatService> chatService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getCommand() != StompCommand.SUBSCRIBE) return message;

        String destination = accessor.getDestination();
        Principal user = accessor.getUser();
        String userId = user == null ? null : user.getName();

        if (isAllowed(destination, userId)) return message;

        log.warn("권한 없는 구독을 차단했습니다. destination={}, userId={}", destination, userId);
        return null;
    }

    boolean isAllowed(String destination, String userId) {
        if (destination == null) return false;

        if (destination.startsWith(CHAT_PREFIX)) {
            if (userId == null) return false;
            // /topic/chat/{roomId}, /topic/chat/{roomId}/read, /topic/chat/{roomId}/translated
            String rest = destination.substring(CHAT_PREFIX.length());
            int slash = rest.indexOf('/');
            String roomId = slash >= 0 ? rest.substring(0, slash) : rest;
            return !roomId.isBlank() && chatService.getObject().canReadRoom(roomId, userId);
        }

        if (destination.startsWith(NOTIFICATION_PREFIX)) {
            // 알림은 본인 채널만 구독할 수 있다.
            return userId != null && userId.equals(destination.substring(NOTIFICATION_PREFIX.length()));
        }

        return true;
    }
}
