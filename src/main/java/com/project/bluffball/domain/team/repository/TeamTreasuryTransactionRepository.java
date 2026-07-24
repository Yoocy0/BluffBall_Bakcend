package com.project.bluffball.domain.team.repository;

import com.project.bluffball.domain.team.entity.TeamTreasuryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 팀 재정 거래 내역 JPA Repository.
 */
public interface TeamTreasuryTransactionRepository extends JpaRepository<TeamTreasuryTransaction, Long> {

    /**
     * 팀의 거래 내역을 최신순으로 조회한다.
     *
     * @param teamId 팀 ID
     * @return 거래 내역 목록
     */
    List<TeamTreasuryTransaction> findByTeamIdOrderByCreatedAtDesc(Long teamId);

    /**
     * 팀의 모든 거래 내역을 삭제한다.
     *
     * @param teamId 팀 ID
     */
    void deleteByTeamId(Long teamId);
}
