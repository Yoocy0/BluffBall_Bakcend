package com.project.bluffball.domain.team.service.usecase.executor;

import com.project.bluffball.domain.team.entity.TeamJoinApplication;
import com.project.bluffball.domain.team.entity.TeamMember;
import com.project.bluffball.domain.team.enums.TeamJoinApplicationStatus;
import com.project.bluffball.domain.team.enums.TeamMemberRole;
import com.project.bluffball.domain.team.repository.TeamJoinApplicationRepository;
import com.project.bluffball.domain.team.repository.TeamMemberRepository;
import com.project.bluffball.domain.team.service.usecase.reader.TeamJoinApplicationReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 팀 가입 신청 쓰기 전담 Executor (usecase/executor 계층).
 */
@Component
@RequiredArgsConstructor
public class TeamJoinApplicationExecutor {

    /** 가입 신청 Repository */
    private final TeamJoinApplicationRepository teamJoinApplicationRepository;

    /** 팀 멤버 Repository */
    private final TeamMemberRepository teamMemberRepository;

    /** 팀 Reader */
    private final TeamReader teamReader;

    /** 가입 신청 Reader */
    private final TeamJoinApplicationReader teamJoinApplicationReader;

    /**
     * PENDING 가입 신청을 생성한다.
     *
     * @param teamId 팀 ID
     * @param userId 신청자 ID
     * @return 생성된 신청 ID
     */
    @Transactional
    public Long apply(Long teamId, Long userId) {
        teamReader.getById(teamId);
        TeamJoinApplication saved =
                teamJoinApplicationRepository.save(new TeamJoinApplication(teamId, userId));
        return saved.getId();
    }

    /**
     * 신청을 승인하고 멤버로 등록한다. 신청자의 다른 PENDING은 취소한다.
     *
     * @param teamId 팀 ID
     * @param applicationId 신청 ID
     * @param reviewerUserId 승인한 리더 ID
     * @return 팀 ID
     */
    @Transactional
    public Long approve(Long teamId, Long applicationId, Long reviewerUserId) {
        TeamJoinApplication application =
                teamJoinApplicationReader.getByIdAndTeamId(applicationId, teamId);
        Long applicantUserId = application.getUserId();

        application.approve(reviewerUserId);
        teamMemberRepository.save(new TeamMember(teamId, applicantUserId, TeamMemberRole.MEMBER));
        cancelOtherPendingApplications(applicantUserId, applicationId);
        return teamId;
    }

    /**
     * 신청을 거부한다.
     *
     * @param teamId 팀 ID
     * @param applicationId 신청 ID
     * @param reviewerUserId 거부한 리더 ID
     */
    @Transactional
    public void reject(Long teamId, Long applicationId, Long reviewerUserId) {
        TeamJoinApplication application =
                teamJoinApplicationReader.getByIdAndTeamId(applicationId, teamId);
        application.reject(reviewerUserId);
    }

    /**
     * 신청자가 본인 신청을 취소한다.
     *
     * @param teamId 팀 ID
     * @param applicationId 신청 ID
     */
    @Transactional
    public void cancel(Long teamId, Long applicationId) {
        TeamJoinApplication application =
                teamJoinApplicationReader.getByIdAndTeamId(applicationId, teamId);
        application.cancel();
    }

    /**
     * 유저의 모든 PENDING 신청을 취소한다 (즉시 가입 시).
     *
     * @param userId 유저 ID
     */
    @Transactional
    public void cancelAllPendingByUser(Long userId) {
        List<TeamJoinApplication> pendings =
                teamJoinApplicationRepository.findByUserIdAndStatus(
                        userId, TeamJoinApplicationStatus.PENDING);
        for (TeamJoinApplication pending : pendings) {
            pending.cancel();
        }
    }

    /**
     * 지정 신청을 제외한 유저의 PENDING 신청을 취소한다.
     *
     * @param userId 유저 ID
     * @param excludeApplicationId 유지할 신청 ID
     */
    private void cancelOtherPendingApplications(Long userId, Long excludeApplicationId) {
        List<TeamJoinApplication> pendings =
                teamJoinApplicationRepository.findByUserIdAndStatus(
                        userId, TeamJoinApplicationStatus.PENDING);
        for (TeamJoinApplication pending : pendings) {
            if (!pending.getId().equals(excludeApplicationId)) {
                pending.cancel();
            }
        }
    }
}
