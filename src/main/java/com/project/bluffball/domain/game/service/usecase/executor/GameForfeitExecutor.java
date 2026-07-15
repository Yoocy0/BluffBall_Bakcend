package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.redis.GameState;
import com.project.bluffball.domain.game.repository.GameStateRepository;
import com.project.bluffball.domain.game.service.usecase.reader.GameProgressReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 접속 끊김 몰수패 점수 반영 Executor.
 */
@Component
@RequiredArgsConstructor
public class GameForfeitExecutor {

    private final GameStateRepository gameStateRepository;
    private final GameProgressReader gameProgressReader;

    public void applyForfeit(String matchSessionId, int homeScore, int awayScore) {
        if (gameProgressReader.isGameOver(matchSessionId)) {
            return;
        }

        GameState gameState = gameProgressReader.getById(matchSessionId);
        gameState.applyForfeitResult(homeScore, awayScore);
        gameStateRepository.save(gameState);
    }
}
