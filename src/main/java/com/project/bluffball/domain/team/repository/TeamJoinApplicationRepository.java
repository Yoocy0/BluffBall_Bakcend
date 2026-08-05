package com.project.bluffball.domain.team.repository;

import com.project.bluffball.domain.team.entity.TeamJoinApplication;
import com.project.bluffball.domain.team.enums.TeamJoinApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 팀 가입 신청 JPA Repository.
 */
public interface TeamJoinApplicationRepository extends JpaRepository<TeamJoinApplication, Long> {

    /**
     * 팀·상태로 신청 목록을 조회한다.
     *
     * @param teamId 팀 ID
     * @param status 상태
     * @return 신청 목록
     */
    List<TeamJoinApplication> findByTeamIdAndStatusOrderByRequestedAtAsc(
            Long teamId,
            TeamJoinApplicationStatus status);

    /**
     * 팀·유저·상태 조합의 신청이 있는지 확인한다.
     *
     * @param teamId 팀 ID
     * @param userId 유저 ID
     * @param status 상태
     * @return 있으면 true
     */
    boolean existsByTeamIdAndUserIdAndStatus(Long teamId, Long userId, TeamJoinApplicationStatus status);

    /**
     * 유저의 특정 상태 신청이 있는지 확인한다.
     *
     * @param userId 유저 ID
     * @param status 상태
     * @return 있으면 true
     */
    boolean existsByUserIdAndStatus(Long userId, TeamJoinApplicationStatus status);

    /**
     * 유저의 특정 상태 신청 목록을 조회한다.
     *
     * @param userId 유저 ID
     * @param status 상태
     * @return 신청 목록
     */
    List<TeamJoinApplication> findByUserIdAndStatus(Long userId, TeamJoinApplicationStatus status);

    /**
     * 신청 ID·팀 ID로 조회한다.
     *
     * @param id 신청 ID
     * @param teamId 팀 ID
     * @return Optional
     */
    Optional<TeamJoinApplication> findByIdAndTeamId(Long id, Long teamId);

    /**
     * 팀의 모든 신청을 삭제한다.
     *
     * @param teamId 팀 ID
     */
    void deleteByTeamId(Long teamId);
}
