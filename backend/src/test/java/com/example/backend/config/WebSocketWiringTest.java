package com.example.backend.config;

import com.example.backend.trma.mapper.ChatMapper;
import com.example.backend.trma.mapper.MoimListMapper;
import com.example.backend.trma.mapper.NotificationMapper;
import com.example.backend.trma.service.NotificationPushService;
import com.example.backend.trma.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

// DB 없이 WebSocket 설정 + 구독 권한 인터셉터 + 실제 ChatServiceImpl만 띄워서,
// 순환 참조 없이 컨텍스트가 뜨고 인터셉터가 inbound 채널에 등록되는지 확인한다.
@SpringJUnitWebConfig({WebSocketConfig.class, StompSubscriptionGuard.class, ChatServiceImpl.class})
@DisplayName("[채팅] WebSocket 설정 배선")
class WebSocketWiringTest {

    @MockitoBean ChatMapper chatMapper;
    @MockitoBean MoimListMapper moimListMapper;
    @MockitoBean NotificationMapper notificationMapper;
    @MockitoBean NotificationPushService notificationPushService;
    @MockitoBean RestClient restClient;

    @Autowired AbstractSubscribableChannel clientInboundChannel;

    @Test
    @DisplayName("구독 권한 인터셉터가 clientInboundChannel에 등록된다")
    void guardRegistered() {
        assertThat(clientInboundChannel.getInterceptors()).hasAtLeastOneElementOfType(StompSubscriptionGuard.class);
    }
}
