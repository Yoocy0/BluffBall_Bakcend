package com.project.bluffball.domain.team.dto.response;

import com.project.bluffball.domain.league.enums.LeagueFormat;

import java.util.List;

/**
 * 리그 출전 로스터 응답 DTO.
 */
public record TeamLineupResponse(
        Long teamId,
        LeagueFormat format,
        List<Long> userIds
) {
}
