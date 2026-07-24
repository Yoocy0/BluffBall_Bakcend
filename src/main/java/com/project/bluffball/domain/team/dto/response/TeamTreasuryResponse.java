package com.project.bluffball.domain.team.dto.response;

import java.util.List;

/**
 * 팀 재정 잔액 및 거래 내역 응답 DTO.
 */
public record TeamTreasuryResponse(
        long treasury,
        List<TeamTreasuryTransactionResponse> transactions
) {
}
