package com.project.bluffball.domain.game.dto.response;

/**
 * 투수 카드 선택 완료 WebSocket 송신 이벤트.
 */
public record PitcherReadyEvent(
        /** 투수가 선택한 시작 좌표 번호 (1~25) */
        int startCoordinateNumber
) {
}
