package com.project.bluffball.gametest.dto;

import com.project.bluffball.domain.game.dto.response.CardInfo;

import java.util.List;

/**
 * 테스트 화면용 — 현재 투수 구종 패 조회/멀리건 결과.
 */
public record TestPitchHandResponse(
        String matchSessionId,
        boolean setupComplete,
        boolean mulliganDone,
        List<CardInfo> pitchHand
) {
}
