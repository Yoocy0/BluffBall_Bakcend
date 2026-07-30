package com.project.bluffball.gametest.dto;

/**
 * 테스트용 리그 투수 교체 결과.
 *
 * @param matchSessionId 매치
 * @param newPitcherUserId 신임 투수
 * @param demotedPitcherUserId 강판 투수
 * @param used 교체 후 사용 횟수
 * @param max 최대 횟수
 */
public record TestSubstitutePitcherResponse(
        String matchSessionId,
        Long newPitcherUserId,
        Long demotedPitcherUserId,
        int used,
        int max
) {
}
