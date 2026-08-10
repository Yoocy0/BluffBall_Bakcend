package com.project.bluffball.domain.game.bot;

/**
 * 투수 봇의 한 투구 결정.
 *
 * @param pitchCardId           핸드에서 고른 구종 카드 ID (인스턴스 또는 마스터)
 * @param startCoordinateNumber 시작 좌표 (1~25)
 */
public record PitcherBotThrowDecision(
        Long pitchCardId,
        int startCoordinateNumber
) {
}
