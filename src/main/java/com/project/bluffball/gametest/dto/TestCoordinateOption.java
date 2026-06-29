package com.project.bluffball.gametest.dto;

/**
 * 테스트 화면용 — 투수가 선택 가능한 시작 좌표 카드.
 */
public record TestCoordinateOption(
        Long cardId,
        int coordinateNumber,
        String name,
        boolean strike
) {
}
