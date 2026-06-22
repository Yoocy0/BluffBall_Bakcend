package com.project.bluffball.domain.game.service.usecase.reader;

import com.project.bluffball.domain.game.dto.response.GameStateSnapshot;
import com.project.bluffball.domain.game.redis.GameState;
import com.project.bluffball.domain.game.repository.GameStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * GameState Redis 엔티티 읽기 전담 리더.
 */
@Component
@RequiredArgsConstructor
public class GameStateReader {

    private final GameStateRepository gameStateRepository;

    /** Executor·Reader 내부 전용 — Service에서 호출 금지 */
    public GameState getById(String matchSessionId) {
        return gameStateRepository.findById(matchSessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "게임 상태를 찾을 수 없습니다. matchSessionId=" + matchSessionId));
    }

    public int getTurnNumber(String matchSessionId) {
        return getById(matchSessionId).getTurnNumber();
    }

    public int getCurrentInning(String matchSessionId) {
        return getById(matchSessionId).getCurrentInning();
    }

    public boolean isTop(String matchSessionId) {
        return getById(matchSessionId).isTop();
    }

    /** Service 이벤트 조립용 스코어보드 스냅샷 (Service ✅) */
    public GameStateSnapshot getSnapshot(String matchSessionId) {
        GameState state = getById(matchSessionId);
        return GameStateSnapshot.builder()
                .inning(state.getCurrentInning())
                .isTop(state.isTop())
                .homeScore(state.getHomeScore())
                .awayScore(state.getAwayScore())
                .balls(state.getBalls())
                .strikes(state.getStrikes())
                .outs(state.getOuts())
                .firstBase(state.isFirstBase())
                .secondBase(state.isSecondBase())
                .thirdBase(state.isThirdBase())
                .build();
    }
}
