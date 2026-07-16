package com.project.bluffball.global.security;

import com.project.bluffball.domain.game.service.MatchPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * STOMP 구독·연결 해제 시 매치 접속 상태를 갱신한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketPresenceEventListener {

    private final GameWebSocketSecurityVerifier gameWebSocketSecurityVerifier;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final MatchPresenceService matchPresenceService;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() == null || accessor.getSessionId() == null) {
            return;
        }

        String matchSessionId = gameWebSocketSecurityVerifier.extractMatchSessionId(accessor.getDestination());
        if (matchSessionId == null) {
            return;
        }

        Long userId = authenticatedUserResolver.requireUserId(accessor.getUser());
        webSocketSessionRegistry.register(accessor.getSessionId(), matchSessionId, userId);
        matchPresenceService.markOnline(matchSessionId, userId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        for (WebSocketSessionRegistry.GameSessionBinding binding
                : webSocketSessionRegistry.removeAll(event.getSessionId())) {
            matchPresenceService.markOffline(binding.matchSessionId(), binding.userId());
            log.debug("WebSocket disconnect matchSessionId={} userId={}",
                    binding.matchSessionId(), binding.userId());
        }
    }
}
