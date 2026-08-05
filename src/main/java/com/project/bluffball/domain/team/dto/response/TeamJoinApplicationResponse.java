package com.project.bluffball.domain.team.dto.response;

import com.project.bluffball.domain.team.enums.TeamJoinApplicationStatus;

import java.time.LocalDateTime;

/**
 * 팀 가입 신청 응답.
 */
public record TeamJoinApplicationResponse(
        Long applicationId,
        Long teamId,
        Long userId,
        String nickname,
        TeamJoinApplicationStatus status,
        LocalDateTime requestedAt,
        LocalDateTime reviewedAt,
        Long reviewedByUserId
) {
}
