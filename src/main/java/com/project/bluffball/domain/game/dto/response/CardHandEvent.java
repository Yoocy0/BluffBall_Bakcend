package com.project.bluffball.domain.game.dto.response;

import java.util.List;

/**
 * 플레이어 카드 패 전달 WebSocket 송신 이벤트.
 */
public record CardHandEvent(
        List<CardInfo> cards,
        Long pitcherUserId,
        Long targetUserId,
        boolean allMulliganReady,
        /** true면 멀리건 확정 응답, false면 초기/공수교대 드로우 */
        boolean fromMulligan
) {
}
