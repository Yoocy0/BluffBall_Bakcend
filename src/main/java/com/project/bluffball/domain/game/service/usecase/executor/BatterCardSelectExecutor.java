package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.game.redis.GameState;
import com.project.bluffball.domain.game.redis.TurnResultSession;
import com.project.bluffball.domain.game.repository.GameStateRepository;
import com.project.bluffball.domain.game.repository.TurnResultSessionRepository;
import com.project.bluffball.domain.game.service.usecase.judgment.BluffingJudgmentCalculator;
import com.project.bluffball.domain.game.service.usecase.judgment.TurnJudgmentCalculator;
import com.project.bluffball.domain.game.service.usecase.judgment.TurnJudgmentResult;
import com.project.bluffball.domain.game.service.usecase.reader.GameStateReader;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.TurnResultSessionReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 타자 선택 저장 및 타격 판정 Executor.
 *
 * <p>야구 카운트·주자·점수 갱신은 {@link com.project.bluffball.domain.game.service.GameProgressService}에 위임한다.</p>
 */
@Component
@RequiredArgsConstructor
public class BatterCardSelectExecutor {

    private final TurnResultSessionRepository turnResultSessionRepository;
    private final TurnResultSessionReader turnResultSessionReader;
    private final GameStateRepository gameStateRepository;
    private final GameStateReader gameStateReader;
    private final MatchInfoReader matchInfoReader;
    private final TurnJudgmentCalculator turnJudgmentCalculator;
    private final BluffingJudgmentCalculator bluffingJudgmentCalculator;

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

        if (result.turnResult() == null && !result.diceResults().isEmpty()) {
            GameState gameState = gameStateReader.getById(matchSessionId);
            boolean hasRunnersOnBase = gameState.isFirstBase()
                    || gameState.isSecondBase()
                    || gameState.isThirdBase();

            var finalTurnResult = bluffingJudgmentCalculator.judge(
                    result.diceResults(),
                    matchInfoReader.getOutNumbers(matchSessionId, session.getCurrentPitcherUserId()),
                    matchInfoReader.getDpNumbers(matchSessionId, session.getCurrentPitcherUserId()),
                    matchInfoReader.getTripleNumbers(matchSessionId, session.getCurrentBatterUserId()),
                    matchInfoReader.getHrNumbers(matchSessionId, session.getCurrentBatterUserId()),
                    hasRunnersOnBase);

            result = new TurnJudgmentResult(
                    finalTurnResult,
                    result.diceResults(),
                    result.selectedTiming(),
                    result.finalCoordinateNumber(),
                    result.pitchTiming(),
                    result.timedOut());
        }

        session.applyBatterTurn(
                batterCoordinateNumber,
                result.selectedTiming(),
                result.diceResults(),
                result.turnResult());
        turnResultSessionRepository.save(session);

        GameState gameState = gameStateReader.getById(matchSessionId);
        gameState.advanceTurn();
        gameStateRepository.save(gameState);

        return result;
    }
}
