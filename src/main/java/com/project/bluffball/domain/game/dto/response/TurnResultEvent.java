package com.project.bluffball.domain.game.dto.response;

import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.game.enums.TurnResult;

import java.util.List;

/**
 * 턴 결과 WebSocket 송신 이벤트.
 */
public record TurnResultEvent(
        TurnResult turnResult,
        int finalCoordinateNumber,
        Timing pitchTiming,
        /** 이번 턴 투수가 선택한 구종명 (예: 포심 패스트볼) */
        String pitchCardName,
        List<Integer> diceResults,
        int inning,
        boolean isTop,
        int homeScore,
        int awayScore,
        int balls,
        int strikes,
        int outs,
        boolean firstBase,
        boolean secondBase,
        boolean thirdBase,
        /** 현재 등판 투수 userId (공수 교대 반영 후) */
        Long pitcherUserId,
        /** 이번 턴으로 초·말 또는 이닝이 바뀌었는지 */
        boolean halfInningChanged,
        /** 경기 종료 여부 */
        boolean gameOver
) {
}
