package com.project.bluffball.domain.game.dto.response;

/**
 * 공수 교대 시 역할(투수 userId) 변경 WebSocket 송신 이벤트.
 *
 * <p>멀리건·카드 드로우 없이 클라이언트 화면만 전환할 때 사용한다.</p>
 */
public record RoleChangedEvent(
        Long pitcherUserId
) {
}
