package com.project.bluffball.domain.game.enums;

/**
 * 매칭 큐 poll 결과에 따른 Service 진입 분기.
 *
 * <p>{@link com.project.bluffball.domain.game.service.usecase.validator.MatchQueueValidator#resolveJoinDecision}이 반환한다.</p>
 */
public enum MatchJoinDecision {

    /** 상대 없음 또는 자기 자신 pop — 큐 대기 등록 */
    WAIT,

    /** 유효한 상대 발견 — 즉시 매치 생성 */
    MATCH
}
