package com.project.bluffball.gametest.bot;

/**
 * 투수 봇의 한 투구 결정.
 *
 * @param pitchCardId 핸드에서 고른 구종 카드 ID
 * @param startCoordinateNumber 시작 좌표 (1~25)
 * @param pitchName 구종명 (디버그용)
 * @param intent 의도 — CHALLENGE(존) / BAIT(유인구)
 */
public record PitcherBotThrowDecision(
        Long pitchCardId,
        int startCoordinateNumber,
        String pitchName,
        Intent intent
) {

    /**
     * 투구 의도.
     */
    public enum Intent {
        /** 존에 꽂아 스트라이크 유인 */
        CHALLENGE,
        /** 존처럼 보이게 한 뒤 빼는 유인구 */
        BAIT
    }
}
