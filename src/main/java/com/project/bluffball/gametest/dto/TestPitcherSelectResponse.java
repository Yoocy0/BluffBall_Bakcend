package com.project.bluffball.gametest.dto;

/**
 * 투수 카드 선택 결과 (테스트 전용).
 * 본番 {@link com.project.bluffball.domain.game.dto.response.PitcherReadyEvent}에는 시작 좌표만 포함된다.
 */
public record TestPitcherSelectResponse(
        String matchSessionId,
        int startCoordinateNumber,
        int finalCoordinateNumber,
        Long pitchCardId,
        Long coordinateCardId,
        String pitchCardName
) {
}
