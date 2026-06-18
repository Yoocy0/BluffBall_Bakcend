package com.project.bluffball.domain.game.event;

/**
 * 한 플레이어의 블러핑 숫자 제출 완료 시 발행되는 이벤트.
 *
 * <p>{@link com.project.bluffball.domain.game.service.listener.GamePhaseListener}가
 * 수신하여 양측 제출 완료 여부를 확인하고, 완료 시 초기 카드 드로우를 시작한다.</p>
 */
public record SetupNumbersSubmittedEvent(String matchSessionId) {
}
