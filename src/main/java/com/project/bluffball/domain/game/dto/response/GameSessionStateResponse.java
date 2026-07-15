package com.project.bluffball.domain.game.dto.response;

import com.project.bluffball.domain.game.enums.GameSessionPhase;
import com.project.bluffball.domain.game.enums.ParticipantRole;
import com.project.bluffball.domain.user.record.enums.GameMode;

import java.util.List;

/**
 * 모바일 재접속용 인게임 세션 스냅샷.
 */
public record GameSessionStateResponse(
        String matchSessionId,
        GameMode gameMode,
        GameSessionPhase phase,
        ParticipantRole myRole,
        Long myUserId,
        Long pitcherUserId,
        Long batterUserId,
        Long opponentUserId,
        List<Long> participantUserIds,
        boolean setupComplete,
        boolean myMulliganDone,
        boolean allMulliganDone,
        List<CardInfo> myCardHand,
        GameBoardStateResponse board,
        GameTurnStateResponse turn,
        GameLastTurnResultResponse lastTurnResult,
        List<ParticipantPresenceResponse> participants
) {
}
