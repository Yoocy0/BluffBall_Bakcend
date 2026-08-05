package com.project.bluffball.domain.team.service.usecase.executor;

import com.project.bluffball.domain.team.entity.Team;
import com.project.bluffball.domain.team.entity.TeamMember;
import com.project.bluffball.domain.team.enums.TeamMemberRole;
import com.project.bluffball.domain.team.repository.TeamJoinApplicationRepository;
import com.project.bluffball.domain.team.repository.TeamMemberRepository;
import com.project.bluffball.domain.team.repository.TeamPresenceRepository;
import com.project.bluffball.domain.team.repository.TeamRepository;
import com.project.bluffball.domain.team.repository.TeamTreasuryTransactionRepository;
import com.project.bluffball.domain.team.service.usecase.reader.TeamMemberReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamReader;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팀 멤버십·삭제 쓰기 전담 Executor (usecase/executor 계층).
 */
@Component
@RequiredArgsConstructor
public class TeamMembershipExecutor {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamTreasuryTransactionRepository teamTreasuryTransactionRepository;
    private final TeamPresenceRepository teamPresenceRepository;
    private final TeamJoinApplicationRepository teamJoinApplicationRepository;
    private final TeamReader teamReader;
    private final TeamMemberReader teamMemberReader;

    /**
     * 팀에 멤버로 가입한다.
     *
     * @param userId 가입 유저 ID
     * @param teamId 팀 ID
     * @return 팀 ID
     */
    @Transactional
    public Long join(Long userId, Long teamId) {
        teamReader.getById(teamId);
        teamMemberRepository.save(new TeamMember(teamId, userId, TeamMemberRole.MEMBER));
        return teamId;
    }

    /**
     * 팀에서 탈퇴한다.
     *
     * @param userId 탈퇴 유저 ID
     * @param teamId 팀 ID
     */
    @Transactional
    public void leave(Long userId, Long teamId) {
        TeamMember member = teamMemberReader.getByTeamIdAndUserId(teamId, userId);
        teamMemberRepository.delete(member);
    }

    /**
     * 멤버를 강제 탈퇴시킨다.
     *
     * @param teamId 팀 ID
     * @param targetUserId 대상 유저 ID
     */
    @Transactional
    public void kick(Long teamId, Long targetUserId) {
        TeamMember member = teamMemberReader.getByTeamIdAndUserId(teamId, targetUserId);
        teamMemberRepository.delete(member);
    }

    /**
     * 멤버 계급을 변경한다. 리더로 변경 시 기존 리더는 MEMBER로 내리고 팀장 ID를 갱신한다.
     *
     * @param teamId 팀 ID
     * @param targetUserId 대상 유저 ID
     * @param newRole 새 계급
     * @return 팀 ID
     */
    @Transactional
    public Long changeRole(Long teamId, Long targetUserId, TeamMemberRole newRole) {
        Team team = teamReader.getById(teamId);
        TeamMember target = teamMemberReader.getByTeamIdAndUserId(teamId, targetUserId);

        if (newRole == TeamMemberRole.LEADER) {
            Long previousLeaderId = team.getLeaderUserId();
            if (!previousLeaderId.equals(targetUserId)) {
                TeamMember previousLeader = teamMemberReader.getByTeamIdAndUserId(teamId, previousLeaderId);
                previousLeader.changeRole(TeamMemberRole.MEMBER);
                team.changeLeader(targetUserId);
            }
        }

        target.changeRole(newRole);
        return teamId;
    }

    /**
     * 팀과 관련 멤버·거래를 삭제한다.
     *
     * @param teamId 팀 ID
     */
    @Transactional
    public void delete(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new NotFoundException(ErrorCode.TEAM_NOT_FOUND, "teamId=" + teamId);
        }
        teamMemberRepository.deleteByTeamId(teamId);
        teamTreasuryTransactionRepository.deleteByTeamId(teamId);
        teamJoinApplicationRepository.deleteByTeamId(teamId);
        teamPresenceRepository.deleteById(String.valueOf(teamId));
        teamRepository.deleteById(teamId);
    }
}
