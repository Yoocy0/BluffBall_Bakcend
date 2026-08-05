package com.project.bluffball.domain.team.dto.response;

import com.project.bluffball.domain.team.enums.TeamJoinApplicationStatus;
import com.project.bluffball.domain.team.enums.TeamJoinOutcome;

/**
 * 팀 가입(즉시 가입 또는 신청 제출) 결과 응답.
 */
public record TeamJoinResponse(
        TeamJoinOutcome outcome,
        TeamResponse team,
        Long applicationId,
        TeamJoinApplicationStatus applicationStatus
) {
}
