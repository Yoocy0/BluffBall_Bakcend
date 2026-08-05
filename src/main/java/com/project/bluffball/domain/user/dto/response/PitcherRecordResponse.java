package com.project.bluffball.domain.user.dto.response;

import com.project.bluffball.domain.user.record.enums.GameMode;

/**
 * 투수 상세 성적 응답.
 *
 * <p>{@code era}는 표시용 계산값이다. ERA = 자책점 × 9 / (innings + outCounts/3).</p>
 */
public record PitcherRecordResponse(
        Long recordId,
        GameMode gameMode,
        Long leagueId,
        int totalGames,
        int wins,
        int loses,
        int innings,
        int outCounts,
        int strikeOuts,
        int baseOnBalls,
        int hits,
        int earnedRuns,
        int doublesAllowed,
        int triplesAllowed,
        int homeRunsAllowed,
        int wildPitches,
        double era
) {
}
