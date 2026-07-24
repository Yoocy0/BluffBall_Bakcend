package com.project.bluffball.domain.team.repository;

import com.project.bluffball.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 팀 JPA Repository.
 */
public interface TeamRepository extends JpaRepository<Team, Long> {

    /**
     * 팀 이름 존재 여부를 확인한다.
     *
     * @param name 팀 이름
     * @return 존재하면 true
     */
    boolean existsByName(String name);

    /**
     * 팀 이름 부분 일치 검색 (대소문자 무시).
     *
     * @param name 검색어
     * @return 매칭 팀 목록
     */
    List<Team> findByNameContainingIgnoreCase(String name);

    /**
     * 팀장 유저 ID로 팀을 조회한다.
     *
     * @param leaderUserId 팀장 유저 ID
     * @return 팀 Optional
     */
    Optional<Team> findByLeaderUserId(Long leaderUserId);
}
