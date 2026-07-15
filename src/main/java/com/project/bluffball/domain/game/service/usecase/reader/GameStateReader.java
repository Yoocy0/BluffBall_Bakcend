package com.project.bluffball.domain.game.service.usecase.reader;

import com.project.bluffball.domain.game.dto.response.GameStateSnapshot;
import com.project.bluffball.domain.game.redis.GameState;
import com.project.bluffball.domain.game.repository.GameStateRepository;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
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
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.GAME_STATE_NOT_FOUND, "matchSessionId=" + matchSessionId));
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

    /** 병살 판정용 — 판정 시점 루상 주자 존재 여부 (Executor·Calculator 입력용) */
    public boolean hasRunnersOnBase(String matchSessionId) {
        GameState state = getById(matchSessionId);
        return state.isFirstBase() || state.isSecondBase() || state.isThirdBase();
    }

    /** Service 이벤트 조립용 스코어보드 스냅샷 (Service ✅) */
    public GameStateSnapshot getSnapshot(String matchSessionId) {
        GameState state = getById(matchSessionId);
        return new GameStateSnapshot(
                state.getCurrentInning(),
                state.isTop(),
                state.getHomeScore(),
                state.getAwayScore(),
                state.getBalls(),
                state.getStrikes(),
                state.getOuts(),
                state.isFirstBase(),
                state.isSecondBase(),
                state.isThirdBase());
    }
}
