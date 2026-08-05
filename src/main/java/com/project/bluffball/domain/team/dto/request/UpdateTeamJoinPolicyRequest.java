package com.project.bluffball.domain.team.dto.request;

import com.project.bluffball.domain.team.enums.TeamJoinPolicy;
import jakarta.validation.constraints.NotNull;

/**
 * 팀 가입 정책 변경 요청.
 */
public record UpdateTeamJoinPolicyRequest(

        @NotNull
        TeamJoinPolicy joinPolicy
) {
}
