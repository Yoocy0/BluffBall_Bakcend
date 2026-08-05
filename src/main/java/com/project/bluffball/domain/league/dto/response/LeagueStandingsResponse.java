package com.project.bluffball.domain.league.dto.response;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;

import java.util.List;

/**
 * 포맷·티어별 리그 전체 순위 응답.
 */
public record LeagueStandingsResponse(
        Long leagueId,
        LeagueFormat format,
        LeagueTier tier,
        String leagueName,
        List<LeagueStandingEntryResponse> standings
) {
}
