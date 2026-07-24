package com.project.bluffball.domain.team.dto.response;

import com.project.bluffball.domain.league.enums.LeagueFormat;

import java.util.List;

/**
 * 리그 출전 로스터 응답 DTO.
 *
 * @param teamId 팀 ID
 * @param format 리그 구분
 * @param userIds 타순
 * @param startingPitcherUserId 선발 투수
 */
public record TeamLineupResponse(
        Long teamId,
        LeagueFormat format,
        List<Long> userIds,
        Long startingPitcherUserId
) {
}
