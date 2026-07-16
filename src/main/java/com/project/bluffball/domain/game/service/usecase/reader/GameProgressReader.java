package com.project.bluffball.domain.game.service.usecase.reader;

import com.project.bluffball.domain.game.dto.progress.GameProgressSituation;
import com.project.bluffball.domain.game.redis.GameState;
import com.project.bluffball.domain.game.repository.GameStateRepository;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 경기 진행 상태 읽기 전담 리더.
 *
 * <p>Service 레이어는 이 클래스의 원시값·DTO 반환 메서드만 호출한다.</p>
 */
@Component
@RequiredArgsConstructor
public class GameProgressReader {

    private final GameStateRepository gameStateRepository;

    /** Executor·Reader 내부 전용 — Service에서 호출 금지 */
    public GameState getById(String matchSessionId) {
        return gameStateRepository.findById(matchSessionId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.GAME_STATE_NOT_FOUND, "matchSessionId=" + matchSessionId));
    }

    /** 현재 경기 상황 스냅샷 (Executor·Calculator 입력용) */
    public GameProgressSituation getSituation(String matchSessionId) {
        GameState state = getById(matchSessionId);
        return new GameProgressSituation(
                state.getTotalInnings(),
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

    /** 총 이닝 수 (Service ✅) */
    public int getTotalInnings(String matchSessionId) {
        return getById(matchSessionId).getTotalInnings();
    }

    /** GameProgressService 초기화 완료 여부 (Service ✅) */
    public boolean isInitialized(String matchSessionId) {
        return getById(matchSessionId).getTotalInnings() > 0;
    }

    /** 경기 종료 여부 (Service ✅) */
    public boolean isGameOver(String matchSessionId) {
        return getById(matchSessionId).isGameOver();
    }
}
