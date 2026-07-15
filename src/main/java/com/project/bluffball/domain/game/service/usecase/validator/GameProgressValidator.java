package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.domain.game.dto.progress.GameTurnOutcome;
import com.project.bluffball.domain.game.enums.TurnResult;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 경기 진행 입력값 검증 (Repository·Entity 접근 없음).
 */
@Component
public class GameProgressValidator {

    public void validateTurnOutcome(GameTurnOutcome outcome) {
        if (outcome == null) {
            throw new BadRequestException(ErrorCode.GAME_INVALID_TURN, "턴 결과가 없습니다.");
        }
        if (outcome.turnNumber() <= 0) {
            throw new BadRequestException(
                    ErrorCode.GAME_INVALID_TURN, "turnNumber=" + outcome.turnNumber());
        }
        if (outcome.turnResult() == null) {
            throw new BadRequestException(ErrorCode.GAME_INVALID_TURN, "TurnResult가 null입니다.");
        }
    }

    public void validateGameActive(boolean initialized, boolean gameOver) {
        if (!initialized) {
            throw new BadRequestException(ErrorCode.GAME_NOT_INITIALIZED);
        }
        if (gameOver) {
            throw new BadRequestException(ErrorCode.GAME_ALREADY_ENDED);
        }
    }

    public void validateApplicableTurnResult(TurnResult turnResult) {
        // enum 전체를 허용 — 추후 모드별 제한 시 여기서 확장
    }
}
