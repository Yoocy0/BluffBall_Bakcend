package com.project.bluffball.domain.team.dto.request;

import com.project.bluffball.domain.team.enums.TeamMemberRole;
import jakarta.validation.constraints.NotNull;

/**
 * 팀원 계급 변경 요청 DTO.
 */
public record UpdateMemberRoleRequest(

        /** 변경할 계급 */
        @NotNull
        TeamMemberRole role
) {
}
