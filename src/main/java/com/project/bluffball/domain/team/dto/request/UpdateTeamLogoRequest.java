package com.project.bluffball.domain.team.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 팀 로고 변경 요청 DTO.
 */
public record UpdateTeamLogoRequest(

        /** 팀 로고 URL */
        @NotBlank
        String logoUrl
) {
}
