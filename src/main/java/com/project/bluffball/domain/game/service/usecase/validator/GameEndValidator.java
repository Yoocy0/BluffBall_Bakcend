package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.domain.game.redis.TurnResultSession;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 경기 종료 후 DB 아카이브 입력값 검증 (Repository·Entity 직접 접근 없음).
 */
@Component
public class GameEndValidator {

    public void validateArchiveReady(boolean initialized, boolean gameEnded, boolean alreadyArchived) {
        if (!initialized) {
            throw new BadRequestException(ErrorCode.GAME_NOT_INITIALIZED);
        }
        if (!gameEnded) {
            throw new BadRequestException(ErrorCode.GAME_NOT_ENDED);
        }
        if (alreadyArchived) {
            throw new BadRequestException(ErrorCode.GAME_ALREADY_ARCHIVED);
        }
    }

    public void validateCompletedTurnSessions(List<TurnResultSession> sessions) {
        if (sessions.isEmpty()) {
            throw new BadRequestException(ErrorCode.GAME_NO_TURN_RESULTS);
        }

        for (TurnResultSession session : sessions) {
            if (session.getTurnResult() == null) {
                throw new BadRequestException(
                        ErrorCode.GAME_INCOMPLETE_TURN, "turnNumber=" + session.getTurnNumber());
            }
            if (session.getSelectedPitchCardId() == null
                    || session.getSelectedCoordinateCardId() == null) {
                throw new BadRequestException(
                        ErrorCode.GAME_INCOMPLETE_PITCHER_SELECTION, "turnNumber=" + session.getTurnNumber());
            }
            if (session.getCurrentPitcherUserId() == null || session.getCurrentBatterUserId() == null) {
                throw new BadRequestException(
                        ErrorCode.GAME_INCOMPLETE_PLAYER_INFO, "turnNumber=" + session.getTurnNumber());
            }
        }
    }
}
