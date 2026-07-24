package com.project.bluffball.domain.team.dto.response;

import com.project.bluffball.domain.team.enums.TeamTreasuryTransactionType;

import java.time.LocalDateTime;

/**
 * 팀 재정 거래 내역 항목 응답 DTO.
 */
public record TeamTreasuryTransactionResponse(
        Long transactionId,
        TeamTreasuryTransactionType transactionType,
        long amount,
        Long sourceUserId,
        Long leagueSeasonId,
        LocalDateTime createdAt
) {
}
