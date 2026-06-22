package com.project.bluffball.domain.game.dto.progress;

import com.project.bluffball.domain.game.enums.TurnResult;

/**
 * {@link com.project.bluffball.domain.game.service.GameTurnService}가
 * {@link com.project.bluffball.domain.game.service.GameProgressService}에 전달하는 턴 판정 입력 DTO.
 *
 * <p>야구 경기 진행 로직은 {@link TurnResult}와 턴 번호만으로 상태를 갱신한다.
 * 좌표·주사위 등 판정 세부값은 포함하지 않는다.</p>
 */
public record GameTurnOutcome(
        TurnResult turnResult,
        int turnNumber
) {
}
