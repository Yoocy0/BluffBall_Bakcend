package com.project.bluffball.domain.user.dto.response;

import com.project.bluffball.domain.user.record.enums.GameMode;

/**
 * 타자 상세 성적 응답.
 *
 * <p>{@code ops}는 표시용 계산값이다.
 * 출루율 = (안타 + 볼넷) / 타석, 장타율 = 루타합 / 타수, OPS = 출루율 + 장타율.</p>
 */
public record BatterRecordResponse(
        Long recordId,
        GameMode gameMode,
        Long leagueId,
        int totalGames,
        int wins,
        int loses,
        int plateAppearances,
        int atBats,
        int hits,
        int baseOnBalls,
        int doubles,
        int triples,
        int homeRuns,
        int runsBattedIn,
        int strikeOuts,
        int doublePlays,
        double ops
) {
}
