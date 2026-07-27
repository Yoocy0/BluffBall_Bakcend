package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.game.dto.response.GameBoardStateResponse;
import com.project.bluffball.domain.game.dto.response.GameLastTurnResultResponse;
import com.project.bluffball.domain.game.dto.response.GameSessionStateResponse;
import com.project.bluffball.domain.game.dto.response.GameStateSnapshot;
import com.project.bluffball.domain.game.dto.response.GameTurnStateResponse;
import com.project.bluffball.domain.game.dto.response.ParticipantPresenceResponse;
import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.enums.GameSessionPhase;
import com.project.bluffball.domain.game.enums.ParticipantRole;
import com.project.bluffball.domain.game.enums.SetupKind;
import com.project.bluffball.domain.game.redis.TurnResultSession;
import com.project.bluffball.domain.game.service.usecase.reader.GameProgressReader;
import com.project.bluffball.domain.game.service.usecase.reader.GameStateReader;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.PitchCardReader;
import com.project.bluffball.domain.game.service.usecase.reader.TurnResultSessionReader;
import com.project.bluffball.domain.game.service.usecase.validator.MatchParticipantValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 모바일 재접속용 인게임 세션 스냅샷 조회.
 */
@Service
@RequiredArgsConstructor
public class GameSessionService {

    private final MatchInfoReader matchInfoReader;
    private final MatchParticipantValidator matchParticipantValidator;
    private final GameStateReader gameStateReader;
    private final GameProgressReader gameProgressReader;
    private final TurnResultSessionReader turnResultSessionReader;
    private final PitchCardReader pitchCardReader;
    private final MatchPresenceService matchPresenceService;

    public GameSessionStateResponse getSessionState(String matchSessionId, Long userId) {
        matchParticipantValidator.validateParticipant(
                matchInfoReader.isParticipant(matchSessionId, userId));

        List<Long> participantUserIds = matchInfoReader.getParticipantUserIds(matchSessionId);
        Long pitcherUserId = matchInfoReader.getPitcherUserId(matchSessionId);
        Long batterUserId = matchInfoReader.getCurrentBatterUserId(matchSessionId);

        boolean setupComplete = matchInfoReader.isSetupNumbersComplete(matchSessionId);
        SetupKind requiredSetupKind = matchInfoReader.getRequiredSetupKind(matchSessionId, userId);
        boolean mySetupComplete = requiredSetupKind == null;
        boolean allMulliganDone = matchInfoReader.isMulliganDone(matchSessionId);
        boolean myMulliganDone = matchInfoReader.isMulliganDoneForUser(matchSessionId, userId);
        boolean gameOver = gameProgressReader.isGameOver(matchSessionId);

        GameSessionPhase phase = resolvePhase(setupComplete, allMulliganDone, gameOver, matchSessionId);
        ParticipantRole myRole = resolveRole(userId, pitcherUserId, batterUserId);

        List<CardInfo> myCardHand = pitchCardReader.getPitchCardDetails(
                matchInfoReader.getPlayerCardHand(matchSessionId, userId));

        List<ParticipantPresenceResponse> participants =
                matchPresenceService.getParticipantPresenceResponses(matchSessionId);

        return new GameSessionStateResponse(
                matchSessionId,
                matchInfoReader.getGameMode(matchSessionId),
                phase,
                myRole,
                userId,
                pitcherUserId,
                batterUserId,
                resolveOpponentUserId(userId, participantUserIds),
                participantUserIds,
                setupComplete,
                requiredSetupKind,
                mySetupComplete,
                myMulliganDone,
                allMulliganDone,
                myCardHand,
                buildBoardState(matchSessionId),
                buildTurnState(matchSessionId, pitcherUserId, batterUserId),
                buildLastTurnResult(matchSessionId).orElse(null),
                participants);
    }

    private GameSessionPhase resolvePhase(boolean setupComplete,
                                          boolean allMulliganDone,
                                          boolean gameOver,
                                          String matchSessionId) {
        if (gameOver) {
            return GameSessionPhase.ENDED;
        }
        if (!setupComplete) {
            return GameSessionPhase.SETUP_NUMBERS;
        }
        if (!allMulliganDone) {
            return GameSessionPhase.MULLIGAN;
        }
        if (!turnResultSessionReader.isPitcherSelectionComplete(matchSessionId)) {
            return GameSessionPhase.PITCHER_SELECT;
        }
        if (!turnResultSessionReader.isBatterSelectionComplete(matchSessionId)) {
            return GameSessionPhase.BATTER_SELECT;
        }
        return GameSessionPhase.PITCHER_SELECT;
    }

    private ParticipantRole resolveRole(Long userId, Long pitcherUserId, Long batterUserId) {
        if (userId.equals(pitcherUserId)) {
            return ParticipantRole.PITCHER;
        }
        return ParticipantRole.BATTER;
    }

    private Long resolveOpponentUserId(Long userId, List<Long> participantUserIds) {
        return participantUserIds.stream()
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElse(null);
    }

    private GameBoardStateResponse buildBoardState(String matchSessionId) {
        GameStateSnapshot snapshot = gameStateReader.getSnapshot(matchSessionId);
        return new GameBoardStateResponse(
                gameStateReader.getTurnNumber(matchSessionId),
                gameProgressReader.getTotalInnings(matchSessionId),
                snapshot.inning(),
                snapshot.isTop(),
                snapshot.homeScore(),
                snapshot.awayScore(),
                snapshot.balls(),
                snapshot.strikes(),
                snapshot.outs(),
                snapshot.firstBase(),
                snapshot.secondBase(),
                snapshot.thirdBase(),
                gameProgressReader.isInitialized(matchSessionId),
                gameProgressReader.isGameOver(matchSessionId));
    }

    private GameTurnStateResponse buildTurnState(String matchSessionId,
                                                 Long pitcherUserId,
                                                 Long batterUserId) {
        int turnNumber = gameStateReader.getTurnNumber(matchSessionId);
        boolean pitcherSelectionComplete =
                turnResultSessionReader.isPitcherSelectionComplete(matchSessionId);
        boolean batterSelectionComplete =
                turnResultSessionReader.isBatterSelectionComplete(matchSessionId);

        int startCoordinateNumber = turnResultSessionReader.findCurrentSession(matchSessionId)
                .map(TurnResultSession::getStartCoordinateNumber)
                .orElse(0);

        return new GameTurnStateResponse(
                turnNumber,
                pitcherUserId,
                batterUserId,
                pitcherSelectionComplete,
                batterSelectionComplete,
                startCoordinateNumber);
    }

    private Optional<GameLastTurnResultResponse> buildLastTurnResult(String matchSessionId) {
        int currentTurnNumber = gameStateReader.getTurnNumber(matchSessionId);
        if (currentTurnNumber <= 1) {
            return Optional.empty();
        }

        return turnResultSessionReader.findSession(matchSessionId, currentTurnNumber - 1)
                .filter(session -> session.getTurnResult() != null)
                .map(session -> new GameLastTurnResultResponse(
                        session.getTurnNumber(),
                        session.getTurnResult(),
                        session.getFinalCoordinateNumber(),
                        session.getPitchTiming(),
                        pitchCardReader.getPitchCardName(session.getSelectedPitchCardId()),
                        session.getDiceResults()));
    }
}
