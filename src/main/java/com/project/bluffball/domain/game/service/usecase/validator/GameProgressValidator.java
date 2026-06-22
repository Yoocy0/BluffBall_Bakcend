package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.domain.game.dto.progress.GameTurnOutcome;
import com.project.bluffball.domain.game.enums.TurnResult;
import org.springframework.stereotype.Component;

/**
 * 경기 진행 입력값 검증 (Repository·Entity 접근 없음).
 */
@Component
public class GameProgressValidator {

    public void validateTurnOutcome(GameTurnOutcome outcome) {
        if (outcome == null) {
            throw new IllegalArgumentException("턴 결과가 없습니다.");
        }
        if (outcome.turnNumber() <= 0) {
            throw new IllegalArgumentException(
                    "턴 번호는 1 이상이어야 합니다. turnNumber=" + outcome.turnNumber());
        }
        if (outcome.turnResult() == null) {
            throw new IllegalArgumentException("TurnResult가 null입니다.");
        }
    }

    public void validateGameActive(boolean initialized, boolean gameOver) {
        if (!initialized) {
            throw new IllegalStateException("경기 진행이 초기화되지 않았습니다.");
        }
        if (gameOver) {
            throw new IllegalStateException("이미 종료된 경기입니다.");
        }
    }

    /**
     * {@link TurnResult}가 경기 진행 엔진에서 처리 가능한 값인지 확인한다.
     */
    public void validateApplicableTurnResult(TurnResult turnResult) {
        // enum 전체를 허용 — 추후 모드별 제한 시 여기서 확장
    }
}
