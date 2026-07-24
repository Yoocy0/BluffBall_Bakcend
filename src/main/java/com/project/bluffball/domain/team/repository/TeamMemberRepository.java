package com.project.bluffball.domain.team.repository;

import com.project.bluffball.domain.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 팀 멤버 JPA Repository.
 */
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    /**
     * 팀의 전체 멤버를 조회한다.
     *
     * @param teamId 팀 ID
     * @return 멤버 목록
     */
    List<TeamMember> findByTeamId(Long teamId);

    /**
     * 유저의 소속 멤버십을 조회한다.
     *
     * @param userId 유저 ID
     * @return 멤버십 Optional
     */
    Optional<TeamMember> findByUserId(Long userId);

    /**
     * 팀·유저 조합으로 멤버십을 조회한다.
     *
     * @param teamId 팀 ID
     * @param userId 유저 ID
     * @return 멤버십 Optional
     */
    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    /**
     * 유저가 이미 팀에 소속되어 있는지 확인한다.
     *
     * @param userId 유저 ID
     * @return 소속이면 true
     */
    boolean existsByUserId(Long userId);

    /**
     * 팀 멤버 수를 반환한다.
     *
     * @param teamId 팀 ID
     * @return 멤버 수
     */
    long countByTeamId(Long teamId);

    /**
     * 팀의 모든 멤버를 삭제한다.
     *
     * @param teamId 팀 ID
     */
    void deleteByTeamId(Long teamId);
}
