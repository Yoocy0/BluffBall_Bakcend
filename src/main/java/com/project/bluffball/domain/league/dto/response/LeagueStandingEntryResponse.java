package com.project.bluffball.domain.league.dto.response;

/**
 * 리그 순위표 한 행.
 */
public record LeagueStandingEntryResponse(
        int rank,
        Long teamId,
        String teamName,
        String logoUrl,
        int rating,
        int wins,
        int losses,
        int runDiff
) {
}
