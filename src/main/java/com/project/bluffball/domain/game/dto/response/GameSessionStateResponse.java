package com.project.bluffball.domain.game.dto.response;

import com.project.bluffball.domain.game.enums.GameSessionPhase;
import com.project.bluffball.domain.game.enums.ParticipantRole;
import com.project.bluffball.domain.game.enums.SetupKind;
import com.project.bluffball.domain.user.record.enums.GameMode;

import java.util.List;

/**
 * 모바일 재접속용 인게임 세션 스냅샷.
 *
 * @param requiredSetupKind 다음에 제출해야 할 셋업 종류 (없으면 null)
 * @param mySetupComplete 요청 유저의 필요 셋업이 모두 끝났는지
 * @param pitcherSubstitution 리그 투수 교체 상태 (쇼다운 등은 max=0)
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
        SetupKind requiredSetupKind,
        boolean mySetupComplete,
        boolean myMulliganDone,
        boolean allMulliganDone,
        List<CardInfo> myCardHand,
        GameBoardStateResponse board,
        GameTurnStateResponse turn,
        GameLastTurnResultResponse lastTurnResult,
        List<ParticipantPresenceResponse> participants,
        PitcherSubstitutionInfo pitcherSubstitution
) {
}
