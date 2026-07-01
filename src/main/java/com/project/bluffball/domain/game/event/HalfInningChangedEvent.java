package com.project.bluffball.domain.game.event;

/**
 * 3아웃으로 공수가 교대되었을 때 발행된다.
 *
 * <p>{@link com.project.bluffball.domain.game.service.listener.GamePhaseListener}가
 * 새 투수 멀리건 준비(역할 교환 + 카드 드로우)를 시작한다.</p>
 */
public record HalfInningChangedEvent(String matchSessionId) {
}
