package com.project.bluffball.domain.team.dto.response;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;

/**
 * 팀 시즌별 리그 기록 항목 응답 DTO.
 */
public record TeamRecordItemResponse(
        Long seasonId,
        Long leagueId,
        LeagueFormat format,
        LeagueTier tier,
        int wins,
        int losses,
        int runDiff,
        int rank,
        int score
) {
}
