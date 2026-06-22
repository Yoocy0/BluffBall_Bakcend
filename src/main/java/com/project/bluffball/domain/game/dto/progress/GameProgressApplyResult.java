package com.project.bluffball.domain.game.dto.progress;

import com.project.bluffball.domain.game.dto.response.GameStateSnapshot;

/**
 * 턴 결과 반영 후 {@link com.project.bluffball.domain.game.service.GameProgressService} 반환 DTO.
 *
 * @param snapshot  갱신된 스코어보드 스냅샷
 * @param gameOver  경기 종료 여부 — true이면 {@code GameEndEvent} 발행 대상
 */
public record GameProgressApplyResult(
        GameStateSnapshot snapshot,
        boolean gameOver
) {
}
