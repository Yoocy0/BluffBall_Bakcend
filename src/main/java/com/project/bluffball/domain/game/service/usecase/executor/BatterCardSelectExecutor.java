package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.enums.TurnResult;
import com.project.bluffball.domain.game.redis.GameState;
import com.project.bluffball.domain.game.redis.TurnResultSession;
import com.project.bluffball.domain.game.repository.GameStateRepository;
import com.project.bluffball.domain.game.repository.TurnResultSessionRepository;
import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.game.service.usecase.judgment.TurnJudgmentCalculator;
import com.project.bluffball.domain.game.service.usecase.judgment.TurnJudgmentResult;
import com.project.bluffball.domain.game.service.usecase.reader.GameStateReader;
import com.project.bluffball.domain.game.service.usecase.reader.TurnResultSessionReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 타자 선택 저장 및 타격 판정 Executor.
 */
@Component
@RequiredArgsConstructor
public class BatterCardSelectExecutor {

    private final TurnResultSessionRepository turnResultSessionRepository;
    private final TurnResultSessionReader turnResultSessionReader;
    private final GameStateRepository gameStateRepository;
    private final GameStateReader gameStateReader;
    private final TurnJudgmentCalculator turnJudgmentCalculator;

    public TurnJudgmentResult execute(String matchSessionId,
                                      int batterCoordinateNumber,
                                      Timing batterTiming,
                                      double responseTimeSec) {
        TurnResultSession session = turnResultSessionReader.getCurrentSession(matchSessionId);

        TurnJudgmentResult result = turnJudgmentCalculator.judge(
                session.getFinalCoordinateNumber(),
                session.getPitchTiming(),
                batterCoordinateNumber,
                batterTiming,
                responseTimeSec);

        session.applyBatterTurn(
                batterCoordinateNumber,
                result.selectedTiming(),
                result.diceResults(),
                result.turnResult());
        turnResultSessionRepository.save(session);

        GameState gameState = gameStateReader.getById(matchSessionId);
        if (result.turnResult() == TurnResult.STRIKE) {
            gameState.addStrike();
        }
        gameState.advanceTurn();
        gameStateRepository.save(gameState);

        return result;
    }
}
