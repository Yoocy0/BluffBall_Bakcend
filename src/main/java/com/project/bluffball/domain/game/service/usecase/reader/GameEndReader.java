package com.project.bluffball.domain.game.service.usecase.reader;

import com.project.bluffball.domain.game.redis.TurnResultSession;
import com.project.bluffball.domain.game.repository.InningLogRepository;
import com.project.bluffball.domain.game.repository.TurnResultSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 경기 종료·아카이브 대상 조회 전담 리더.
 */
@Component
@RequiredArgsConstructor
public class GameEndReader {

    private final GameProgressReader gameProgressReader;
    private final GameStateReader gameStateReader;
    private final TurnResultSessionRepository turnResultSessionRepository;
    private final InningLogRepository inningLogRepository;

    /**
     * 경기가 종료됐는지 여부 (끝내기·규정/연장 말 종료 후 승패 확정·몰수 등).
     */
    public boolean isGameEnded(String matchSessionId) {
        return gameProgressReader.isGameOver(matchSessionId);
    }

    /** MariaDB에 이미 InningLog로 저장됐는지 여부 */
    public boolean isAlreadyArchived(String matchSessionId) {
        return inningLogRepository.existsByMatchSessionId(matchSessionId);
    }

    /** 완료된 턴(타자 판정까지 끝난 턴)의 TurnResultSession 목록 — 턴 번호 오름차순 */
    public List<TurnResultSession> getCompletedTurnSessions(String matchSessionId) {
        int lastCompletedTurn = gameStateReader.getTurnNumber(matchSessionId) - 1;
        if (lastCompletedTurn < 1) {
            return List.of();
        }

        List<TurnResultSession> sessions = new ArrayList<>(lastCompletedTurn);
        for (int turnNumber = 1; turnNumber <= lastCompletedTurn; turnNumber++) {
            turnResultSessionRepository.findById(matchSessionId + ":" + turnNumber)
                    .ifPresent(sessions::add);
        }
        return sessions;
    }
}
