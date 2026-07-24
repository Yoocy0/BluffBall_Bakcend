package com.project.bluffball.domain.team.service.usecase.reader;

import com.project.bluffball.domain.team.dto.response.TeamTreasuryResponse;
import com.project.bluffball.domain.team.dto.response.TeamTreasuryTransactionResponse;
import com.project.bluffball.domain.team.entity.TeamTreasuryTransaction;
import com.project.bluffball.domain.team.repository.TeamTreasuryTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 팀 재정 읽기 전담 Reader (usecase/reader 계층).
 */
@Component
@RequiredArgsConstructor
public class TeamTreasuryReader {

    private final TeamTreasuryTransactionRepository teamTreasuryTransactionRepository;
    private final TeamReader teamReader;

    /**
     * 팀 재정 잔액과 거래 내역을 반환한다.
     *
     * @param teamId 팀 ID
     * @return 재정 응답 DTO
     */
    public TeamTreasuryResponse getTreasuryResponse(Long teamId) {
        long treasury = teamReader.getTreasury(teamId);
        List<TeamTreasuryTransactionResponse> transactions =
                teamTreasuryTransactionRepository.findByTeamIdOrderByCreatedAtDesc(teamId).stream()
                        .map(this::toResponse)
                        .toList();
        return new TeamTreasuryResponse(treasury, transactions);
    }

    /**
     * 거래 Entity → 응답 DTO 변환.
     *
     * @param transaction 거래 Entity
     * @return 거래 응답 DTO
     */
    private TeamTreasuryTransactionResponse toResponse(TeamTreasuryTransaction transaction) {
        return new TeamTreasuryTransactionResponse(
                transaction.getId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getSourceUserId(),
                transaction.getLeagueSeasonId(),
                transaction.getCreatedAt());
    }
}
