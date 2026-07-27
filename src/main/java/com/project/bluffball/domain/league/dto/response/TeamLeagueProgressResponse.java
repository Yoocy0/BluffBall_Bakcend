package com.project.bluffball.domain.league.dto.response;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;

/**
 * 팀의 포맷별 리그 진행 상태 응답.
 */
public record TeamLeagueProgressResponse(
        Long teamId,
        LeagueFormat format,
        LeagueTier currentTier,
        int rating,
        int ratingFloor,
        int ratingCeil,
        boolean promoteReady,
        LeagueTier nextTier,
        int wins,
        int losses,
        int runDiff
) {
}
