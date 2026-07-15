package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.game.service.usecase.executor.GameForfeitExecutor;
import com.project.bluffball.domain.game.service.usecase.reader.GameProgressReader;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 접속 끊김 몰수패(10:0) 처리.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameForfeitService {

    public static final int FORFEIT_WINNER_SCORE = 10;
    public static final int FORFEIT_LOSER_SCORE = 0;

    private final GameForfeitExecutor gameForfeitExecutor;
    private final GameProgressReader gameProgressReader;
    private final MatchInfoReader matchInfoReader;
    private final GameProgressService gameProgressService;

    /**
     * 끊긴 참가자를 패배 처리하고 10:0으로 경기를 종료한다.
     */
    public void applyDisconnectForfeit(String matchSessionId, Long disconnectedUserId) {
        if (gameProgressReader.isGameOver(matchSessionId)) {
            return;
        }

        Long homeUserId = matchInfoReader.getHomeUserId(matchSessionId);
        Long awayUserId = matchInfoReader.getAwayUserId(matchSessionId);
        Long winnerUserId = disconnectedUserId.equals(homeUserId) ? awayUserId : homeUserId;

        int homeScore = winnerUserId.equals(homeUserId) ? FORFEIT_WINNER_SCORE : FORFEIT_LOSER_SCORE;
        int awayScore = winnerUserId.equals(awayUserId) ? FORFEIT_WINNER_SCORE : FORFEIT_LOSER_SCORE;

        gameForfeitExecutor.applyForfeit(matchSessionId, homeScore, awayScore);
        gameProgressService.finalizeGameEnd(matchSessionId, winnerUserId);

        log.info("접속 끊김 몰수패 처리 matchSessionId={} disconnectedUserId={} winnerUserId={} score={}:{}",
                matchSessionId, disconnectedUserId, winnerUserId, homeScore, awayScore);
    }
}
