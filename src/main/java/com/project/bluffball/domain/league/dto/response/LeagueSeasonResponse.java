package com.project.bluffball.domain.league.dto.response;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueSeasonStatus;
import com.project.bluffball.domain.league.enums.LeagueTier;

import java.time.LocalDateTime;

/**
 * 리그 시즌 응답 DTO.
 */
public record LeagueSeasonResponse(
        Long seasonId,
        Long leagueId,
        LeagueFormat format,
        LeagueTier tier,
        String periodLabel,
        int groupNumber,
        LeagueSeasonStatus status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        int maxTeams,
        int joinedTeamCount
) {
}
