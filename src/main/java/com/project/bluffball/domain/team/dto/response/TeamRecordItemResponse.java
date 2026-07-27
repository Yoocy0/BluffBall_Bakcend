package com.project.bluffball.domain.team.dto.response;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;

/**
 * 팀 포맷별 리그 기록 항목 응답 DTO.
 */
public record TeamRecordItemResponse(
        Long leagueId,
        LeagueFormat format,
        LeagueTier tier,
        int wins,
        int losses,
        int runDiff,
        int rating
) {
}
