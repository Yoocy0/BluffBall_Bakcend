package com.project.bluffball.domain.team.service.usecase.reader;

import com.project.bluffball.domain.team.dto.response.TeamJoinApplicationResponse;
import com.project.bluffball.domain.team.entity.TeamJoinApplication;
import com.project.bluffball.domain.team.enums.TeamJoinApplicationStatus;
import com.project.bluffball.domain.team.repository.TeamJoinApplicationRepository;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 팀 가입 신청 읽기 전담 Reader (usecase/reader 계층).
 */
@Component
@RequiredArgsConstructor
public class TeamJoinApplicationReader {

    /** 가입 신청 Repository */
    private final TeamJoinApplicationRepository teamJoinApplicationRepository;

    /** 유저 닉네임 조회용 Reader */
    private final UserReader userReader;

    /**
     * Entity 조회 (Executor·Reader 내부용).
     *
     * @param applicationId 신청 ID
     * @param teamId 팀 ID
     * @return 신청 Entity
     */
    public TeamJoinApplication getByIdAndTeamId(Long applicationId, Long teamId) {
        return teamJoinApplicationRepository.findByIdAndTeamId(applicationId, teamId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.TEAM_JOIN_APPLICATION_NOT_FOUND,
                        "applicationId=" + applicationId + ", teamId=" + teamId));
    }

    /**
     * 팀에 대한 PENDING 신청이 있는지 확인한다.
     *
     * @param teamId 팀 ID
     * @param userId 유저 ID
     * @return 있으면 true
     */
    public boolean hasPendingForTeam(Long teamId, Long userId) {
        return teamJoinApplicationRepository.existsByTeamIdAndUserIdAndStatus(
                teamId, userId, TeamJoinApplicationStatus.PENDING);
    }

    /**
     * 유저에게 PENDING 신청이 있는지 확인한다.
     *
     * @param userId 유저 ID
     * @return 있으면 true
     */
    public boolean hasAnyPending(Long userId) {
        return teamJoinApplicationRepository.existsByUserIdAndStatus(
                userId, TeamJoinApplicationStatus.PENDING);
    }

    /**
     * 팀의 PENDING 신청 목록을 반환한다. (Service ✅)
     *
     * @param teamId 팀 ID
     * @return 신청 응답 목록
     */
    public List<TeamJoinApplicationResponse> getPendingResponses(Long teamId) {
        return teamJoinApplicationRepository
                .findByTeamIdAndStatusOrderByRequestedAtAsc(teamId, TeamJoinApplicationStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 신청 응답 DTO를 반환한다. (Service ✅)
     *
     * @param applicationId 신청 ID
     * @param teamId 팀 ID
     * @return 신청 응답
     */
    public TeamJoinApplicationResponse getResponse(Long applicationId, Long teamId) {
        return toResponse(getByIdAndTeamId(applicationId, teamId));
    }

    /**
     * Entity → DTO.
     *
     * @param application 신청 Entity
     * @return 응답 DTO
     */
    private TeamJoinApplicationResponse toResponse(TeamJoinApplication application) {
        return new TeamJoinApplicationResponse(
                application.getId(),
                application.getTeamId(),
                application.getUserId(),
                userReader.getNickname(application.getUserId()),
                application.getStatus(),
                application.getRequestedAt(),
                application.getReviewedAt(),
                application.getReviewedByUserId());
    }
}
