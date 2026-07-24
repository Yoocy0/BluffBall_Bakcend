package com.project.bluffball.domain.team.dto.response;

/**
 * 팀 정보 응답 DTO.
 */
public record TeamResponse(
        Long teamId,
        String name,
        String logoUrl,
        Long leaderUserId,
        long treasury,
        Long currentLeagueSeasonId,
        Long currentLeagueId
) {
}
