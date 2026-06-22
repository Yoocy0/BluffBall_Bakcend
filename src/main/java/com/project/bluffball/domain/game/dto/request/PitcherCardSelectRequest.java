package com.project.bluffball.domain.game.dto.request;

/**
 * 투수 카드 선택 WebSocket 수신 DTO.
 */
public record PitcherCardSelectRequest(
        /** 선택한 구종 카드 ID */
        Long pitchCardId,
        /** 선택한 시작 좌표 카드 ID */
        Long coordinateCardId
) {
}
