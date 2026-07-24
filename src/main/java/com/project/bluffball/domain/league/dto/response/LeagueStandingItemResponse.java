package com.project.bluffball.domain.league.dto.response;

/**
 * 리그 시즌 순위표 항목 응답 DTO.
 */
public record LeagueStandingItemResponse(
        int rank,
        Long teamId,
        String teamName,
        String logoUrl,
        int wins,
        int losses,
        int runDiff,
        int score
) {
}
