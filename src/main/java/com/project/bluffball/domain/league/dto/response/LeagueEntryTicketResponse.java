package com.project.bluffball.domain.league.dto.response;

/**
 * 리그 참여권 구매 응답 DTO.
 */
public record LeagueEntryTicketResponse(
        Long ticketId,
        Long teamId,
        Long seasonId,
        long feeAmount
) {
}
