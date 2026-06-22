package com.project.bluffball.domain.game.service.usecase.reader;

import com.project.bluffball.domain.game.redis.TurnResultSession;
import com.project.bluffball.domain.game.repository.TurnResultSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * TurnResultSession Redis 엔티티 읽기 전담 리더.
 */
@Component
@RequiredArgsConstructor
public class TurnResultSessionReader {

    private final TurnResultSessionRepository turnResultSessionRepository;
    private final GameStateReader gameStateReader;

    /** Executor·Reader 내부 전용 — Service에서 호출 금지 */
    public TurnResultSession getById(String sessionId) {
        return turnResultSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "턴 세션을 찾을 수 없습니다. sessionId=" + sessionId));
    }

    /** 현재 턴의 TurnResultSession 반환 (Executor·Reader 내부) */
    public TurnResultSession getCurrentSession(String matchSessionId) {
        int turnNumber = gameStateReader.getTurnNumber(matchSessionId);
        return getById(matchSessionId + ":" + turnNumber);
    }

    /** 현재 턴에 투수 카드 선택이 완료되었는지 (Service ✅) */
    public boolean isPitcherSelectionComplete(String matchSessionId) {
        return findCurrentSession(matchSessionId)
                .map(session -> session.getSelectedPitchCardId() != null
                        && session.getSelectedCoordinateCardId() != null)
                .orElse(false);
    }

    /** 현재 턴에 타자 선택·판정이 완료되었는지 (Service ✅) */
    public boolean isBatterSelectionComplete(String matchSessionId) {
        return findCurrentSession(matchSessionId)
                .map(session -> session.getTurnResult() != null)
                .orElse(false);
    }

    private Optional<TurnResultSession> findCurrentSession(String matchSessionId) {
        int turnNumber = gameStateReader.getTurnNumber(matchSessionId);
        return turnResultSessionRepository.findById(matchSessionId + ":" + turnNumber);
    }
}
