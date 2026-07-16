package com.project.bluffball.domain.game.dto.response;

/**
 * 상대 재접속 WebSocket 송신 이벤트.
 */
public record OpponentReconnectedEvent(
        Long reconnectedUserId
) {
}
