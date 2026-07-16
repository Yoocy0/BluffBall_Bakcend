package com.project.bluffball.global.security;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * WebSocket 세션 ↔ 매치 참가 바인딩 (인메모리).
 */
@Component
public class WebSocketSessionRegistry {

    private final ConcurrentMap<String, Set<GameSessionBinding>> bindingsBySessionId = new ConcurrentHashMap<>();

    public void register(String webSocketSessionId, String matchSessionId, Long userId) {
        bindingsBySessionId
                .computeIfAbsent(webSocketSessionId, ignored -> ConcurrentHashMap.newKeySet())
                .add(new GameSessionBinding(matchSessionId, userId));
    }

    public List<GameSessionBinding> removeAll(String webSocketSessionId) {
        Set<GameSessionBinding> bindings = bindingsBySessionId.remove(webSocketSessionId);
        if (bindings == null || bindings.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(bindings);
    }

    public record GameSessionBinding(String matchSessionId, Long userId) {
    }
}
