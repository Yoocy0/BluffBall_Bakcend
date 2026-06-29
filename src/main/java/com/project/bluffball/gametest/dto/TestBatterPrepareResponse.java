package com.project.bluffball.gametest.dto;

/**
 * 타자 선택 화면 진입용 — 투수가 공개한 시작 좌표.
 */
public record TestBatterPrepareResponse(
        String matchSessionId,
        int startCoordinateNumber,
        boolean pitcherSelectionComplete,
        boolean batterSelectionComplete
) {
}
