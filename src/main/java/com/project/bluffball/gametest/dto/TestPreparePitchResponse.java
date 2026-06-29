package com.project.bluffball.gametest.dto;

import com.project.bluffball.domain.game.dto.response.CardInfo;

import java.util.List;

/**
 * 투수 선택 화면 진입 전 게임 준비 결과 (테스트 전용).
 */
public record TestPreparePitchResponse(
        String matchSessionId,
        boolean setupComplete,
        boolean mulliganDone,
        List<CardInfo> pitchHand,
        List<TestCoordinateOption> coordinateOptions
) {
}
