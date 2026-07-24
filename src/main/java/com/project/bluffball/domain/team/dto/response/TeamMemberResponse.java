package com.project.bluffball.domain.team.dto.response;

import com.project.bluffball.domain.team.enums.TeamMemberRole;

import java.time.LocalDateTime;

/**
 * 팀 멤버 응답 DTO.
 */
public record TeamMemberResponse(
        Long userId,
        String nickname,
        TeamMemberRole role,
        boolean online,
        LocalDateTime joinedAt
) {
}
