package com.project.bluffball.domain.game.dto.response;

/**
 * 현재 턴 선택 진행 상태.
 */
public record GameTurnStateResponse(
        int turnNumber,
        Long pitcherUserId,
        Long batterUserId,
        boolean pitcherSelectionComplete,
        boolean batterSelectionComplete,
        /** 투수 선택 완료 시 타자 화면용 시작 좌표 (1~25). 미선택 시 0 */
        int startCoordinateNumber
) {
}
