package com.project.bluffball.domain.team.dto.response;

import com.project.bluffball.domain.team.enums.TeamJoinPolicy;

/**
 * 팀 정보 응답 DTO.
 */
public record TeamResponse(
        Long teamId,
        String name,
        String logoUrl,
        Long leaderUserId,
        long treasury,
        Long currentLeagueId,
        TeamJoinPolicy joinPolicy
) {
}
