package com.project.bluffball.domain.game.service.usecase.reader;

import com.project.bluffball.domain.game.dto.response.GameStateSnapshot;
import com.project.bluffball.domain.game.redis.GameState;
import com.project.bluffball.domain.game.redis.TurnResultSession;
import com.project.bluffball.domain.game.repository.GameStateRepository;
import com.project.bluffball.domain.game.repository.TurnResultSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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

    /** 투수 선택이 완료된 현재 턴 세션인지 확인 (Service ✅) */
    public boolean isPitcherSelectionComplete(String matchSessionId) {
        TurnResultSession session = getCurrentSession(matchSessionId);
        return session.getSelectedPitchCardId() != null
                && session.getSelectedCoordinateCardId() != null;
    }
}
