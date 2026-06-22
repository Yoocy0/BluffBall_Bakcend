package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.dto.progress.GameTurnOutcome;
import com.project.bluffball.domain.game.redis.GameState;
import com.project.bluffball.domain.game.repository.GameStateRepository;
import com.project.bluffball.domain.game.service.usecase.judgment.GameProgressCalculator;
import com.project.bluffball.domain.game.service.usecase.reader.GameProgressReader;
import com.project.bluffball.domain.game.service.usecase.validator.GameProgressValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 경기 진행 상태(Redis GameState) 쓰기 전담 Executor.
 */
@Component
@RequiredArgsConstructor
public class GameProgressExecutor {

    private final GameStateRepository gameStateRepository;
    private final GameProgressReader gameProgressReader;
    private final GameProgressValidator gameProgressValidator;
    private final GameProgressCalculator gameProgressCalculator;

    /**
     * 경기 진행 규칙(총 이닝 수)을 등록하고 스코어보드를 초기 상태로 세팅한다.
     */
    public void initialize(String matchSessionId, int totalInnings) {
        GameState gameState = gameProgressReader.getById(matchSessionId);
        gameState.initializeProgress(totalInnings);
        gameStateRepository.save(gameState);
    }

    /**
     * 한 턴의 {@link com.project.bluffball.domain.game.enums.TurnResult}를 야구 룰에 따라 반영한다.
     *
     * <p>결과별 분기는 {@link GameProgressCalculator#apply}에서 처리한다.</p>
     */
    public void applyTurnResult(String matchSessionId, GameTurnOutcome outcome) {
        gameProgressValidator.validateTurnOutcome(outcome);
        gameProgressValidator.validateApplicableTurnResult(outcome.turnResult());
        gameProgressValidator.validateGameActive(
                gameProgressReader.isInitialized(matchSessionId),
                gameProgressReader.isGameOver(matchSessionId));

        // 현재 스코어보드 + TurnResult → 다음 상태 (GameProgressCalculator.switch 분기)
        GameProgressCalculator.Transition transition = gameProgressCalculator.apply(
                gameProgressReader.getSituation(matchSessionId),
                outcome.turnResult());

        // 계산 결과를 Redis GameState에 저장
        GameState gameState = gameProgressReader.getById(matchSessionId);
        gameState.applyProgressSituation(transition.after(), transition.gameOver());
        gameStateRepository.save(gameState);
    }
}
