package com.project.bluffball.global.config;

import com.project.bluffball.global.security.WebSocketAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * STOMP over WebSocket 설정.
 *
 * <p>연결 흐름</p>
 * <pre>
 * 클라이언트 연결 엔드포인트 : /ws  (SockJS fallback 지원)
 * 클라이언트 → 서버 전송    : /app/{destination}
 * 서버 → 클라이언트 구독    : /topic/{destination}
 * </pre>
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthChannelInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버 → 클라이언트 푸시 채널 prefix
        registry.enableSimpleBroker("/topic");
        // 클라이언트 → 서버 전송 prefix (@MessageMapping 핸들러로 라우팅)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /** @Payload JSON 역직렬화 — content-type: application/json */
    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        messageConverters.add(new MappingJackson2MessageConverter());
        return false;
    }
}
