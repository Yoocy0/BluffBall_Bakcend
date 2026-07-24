package com.project.bluffball.domain.team.dto.request;

import jakarta.validation.constraints.Positive;

/**
 * 팀 재화 기부 요청 DTO.
 */
public record DonateTeamRequest(

        /** 기부 금액 (유저 재화 → 팀 재정) */
        @Positive
        long amount
) {
}
