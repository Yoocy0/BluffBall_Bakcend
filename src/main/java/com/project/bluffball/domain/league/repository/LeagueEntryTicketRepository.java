package com.project.bluffball.domain.league.repository;

import com.project.bluffball.domain.league.entity.LeagueEntryTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 리그 참여권 JPA Repository.
 */
public interface LeagueEntryTicketRepository extends JpaRepository<LeagueEntryTicket, Long> {

    /**
     * 팀·시즌 조합의 참여권 존재 여부를 반환한다.
     *
     * @param teamId 팀 ID
     * @param leagueSeasonId 시즌 ID
     * @return 존재하면 true
     */
    boolean existsByTeamIdAndLeagueSeasonId(Long teamId, Long leagueSeasonId);

    /**
     * 팀·시즌 조합의 참여권을 조회한다.
     *
     * @param teamId 팀 ID
     * @param leagueSeasonId 시즌 ID
     * @return 참여권 Optional
     */
    Optional<LeagueEntryTicket> findByTeamIdAndLeagueSeasonId(Long teamId, Long leagueSeasonId);
}
