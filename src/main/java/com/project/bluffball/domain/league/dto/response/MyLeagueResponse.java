package com.project.bluffball.domain.league.dto.response;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueSeasonStatus;
import com.project.bluffball.domain.league.enums.LeagueTier;

/**
 * 내 팀 현재 소속 리그/시즌 응답 DTO.
 */
public record MyLeagueResponse(
        Long teamId,
        Long seasonId,
        Long leagueId,
        LeagueFormat format,
        LeagueTier tier,
        LeagueSeasonStatus status,
        int rank,
        int wins,
        int losses,
        int runDiff,
        int score
) {
}
