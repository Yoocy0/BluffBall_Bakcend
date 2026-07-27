package com.project.bluffball.domain.team.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 팀 창단 요청 DTO.
 */
public record CreateTeamRequest(

        /** 팀 이름 (unique, 이후 변경 불가). 표시 폭 ≤ 16(한글 8자) */
        @NotBlank
        @Size(max = 16)
        String name,

        /** 팀 로고 URL (선택) */
        String logoUrl
) {
}
