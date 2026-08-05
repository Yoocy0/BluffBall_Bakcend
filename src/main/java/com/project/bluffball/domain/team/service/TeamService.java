package com.project.bluffball.domain.team.service;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.team.dto.request.CreateTeamRequest;
import com.project.bluffball.domain.team.dto.request.DonateTeamRequest;
import com.project.bluffball.domain.team.dto.request.UpdateMemberRoleRequest;
import com.project.bluffball.domain.team.dto.request.UpdateTeamJoinPolicyRequest;
import com.project.bluffball.domain.team.dto.request.UpdateTeamLogoRequest;
import com.project.bluffball.domain.team.dto.response.TeamJoinApplicationResponse;
import com.project.bluffball.domain.team.dto.response.TeamJoinResponse;
import com.project.bluffball.domain.team.dto.response.TeamMemberResponse;
import com.project.bluffball.domain.team.dto.response.TeamRecordsResponse;
import com.project.bluffball.domain.team.dto.response.TeamResponse;
import com.project.bluffball.domain.team.dto.response.TeamTreasuryResponse;
import com.project.bluffball.domain.team.enums.TeamJoinOutcome;
import com.project.bluffball.domain.team.enums.TeamJoinPolicy;
import com.project.bluffball.domain.team.enums.TeamMemberRole;
import com.project.bluffball.domain.team.service.usecase.executor.TeamCreateExecutor;
import com.project.bluffball.domain.team.service.usecase.executor.TeamJoinApplicationExecutor;
import com.project.bluffball.domain.team.service.usecase.executor.TeamJoinPolicyExecutor;
import com.project.bluffball.domain.team.service.usecase.executor.TeamLogoExecutor;
import com.project.bluffball.domain.team.service.usecase.executor.TeamMembershipExecutor;
import com.project.bluffball.domain.team.service.usecase.executor.TeamPresenceExecutor;
import com.project.bluffball.domain.team.service.usecase.executor.TeamTreasuryExecutor;
import com.project.bluffball.domain.team.service.usecase.reader.TeamJoinApplicationReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamMemberReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamRecordReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamTreasuryReader;
import com.project.bluffball.domain.team.service.usecase.validator.TeamCreateValidator;
import com.project.bluffball.domain.team.service.usecase.validator.TeamDonateValidator;
import com.project.bluffball.domain.team.service.usecase.validator.TeamMembershipValidator;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 팀(클랜) 서비스.
 *
 * <p>팀 관련 API 유스케이스를 validator·reader·executor에 위임하여 조립한다.
 * Entity·Repository에 직접 접근하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamCreateValidator teamCreateValidator;
    private final TeamMembershipValidator teamMembershipValidator;
    private final TeamDonateValidator teamDonateValidator;

    private final TeamReader teamReader;
    private final TeamMemberReader teamMemberReader;
    private final TeamTreasuryReader teamTreasuryReader;
    private final TeamRecordReader teamRecordReader;
    private final TeamJoinApplicationReader teamJoinApplicationReader;
    private final UserReader userReader;

    private final TeamCreateExecutor teamCreateExecutor;
    private final TeamMembershipExecutor teamMembershipExecutor;
    private final TeamTreasuryExecutor teamTreasuryExecutor;
    private final TeamLogoExecutor teamLogoExecutor;
    private final TeamPresenceExecutor teamPresenceExecutor;
    private final TeamJoinApplicationExecutor teamJoinApplicationExecutor;
    private final TeamJoinPolicyExecutor teamJoinPolicyExecutor;

    /**
     * 팀을 창단한다.
     *
     * @param userId 창단자 유저 ID
     * @param request 창단 요청
     * @return 생성된 팀 정보
     */
    public TeamResponse create(Long userId, CreateTeamRequest request) {
        String name = request.name().trim();
        teamCreateValidator.validateName(name);
        teamCreateValidator.validateNameNotDuplicated(teamReader.existsByName(name));
        teamCreateValidator.validateNotAlreadyJoined(teamMemberReader.existsByUserId(userId));

        TeamJoinPolicy joinPolicy =
                request.joinPolicy() != null ? request.joinPolicy() : TeamJoinPolicy.OPEN;
        Long teamId = teamCreateExecutor.create(name, userId, request.logoUrl(), joinPolicy);
        return teamReader.getTeamResponse(teamId);
    }

    /**
     * 내 소속 팀을 조회한다.
     *
     * @param userId 유저 ID
     * @return 팀 정보
     * @throws NotFoundException 소속 팀이 없으면
     */
    public TeamResponse getMyTeam(Long userId) {
        Long teamId = teamMemberReader.findTeamIdByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TEAM_NOT_FOUND, "userId=" + userId));
        return teamReader.getTeamResponse(teamId);
    }

    /**
     * 팀 이름으로 검색한다.
     *
     * @param name 검색어 (nullable)
     * @return 팀 목록
     */
    public List<TeamResponse> search(String name) {
        return teamReader.search(name);
    }

    /**
     * 팀 상세를 조회한다.
     *
     * @param teamId 팀 ID
     * @return 팀 정보
     */
    public TeamResponse getTeam(Long teamId) {
        return teamReader.getTeamResponse(teamId);
    }

    /**
     * 팀을 삭제한다. 리더만 가능하다.
     *
     * @param userId 요청 유저 ID
     * @param teamId 팀 ID
     */
    public void delete(Long userId, Long teamId) {
        teamReader.getTeamResponse(teamId);
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, userId));
        teamMembershipExecutor.delete(teamId);
    }

    /**
     * 팀에 가입하거나 가입 신청을 제출한다.
     *
     * <p>{@link TeamJoinPolicy#OPEN}이면 즉시 가입, {@link TeamJoinPolicy#APPROVAL_REQUIRED}이면 PENDING 신청.</p>
     *
     * @param userId 가입 유저 ID
     * @param teamId 팀 ID
     * @return 가입/신청 결과
     */
    public TeamJoinResponse join(Long userId, Long teamId) {
        TeamResponse team = teamReader.getTeamResponse(teamId);
        teamMembershipValidator.validateCanJoin(teamMemberReader.existsByUserId(userId));
        teamMembershipValidator.validateCapacity(teamMemberReader.countMembers(teamId));

        if (team.joinPolicy() == TeamJoinPolicy.APPROVAL_REQUIRED) {
            teamMembershipValidator.validateNoPendingApplication(
                    teamJoinApplicationReader.hasAnyPending(userId));
            Long applicationId = teamJoinApplicationExecutor.apply(teamId, userId);
            return new TeamJoinResponse(
                    TeamJoinOutcome.APPLICATION_SUBMITTED,
                    team,
                    applicationId,
                    teamJoinApplicationReader.getResponse(applicationId, teamId).status());
        }

        teamJoinApplicationExecutor.cancelAllPendingByUser(userId);
        Long joinedTeamId = teamMembershipExecutor.join(userId, teamId);
        return new TeamJoinResponse(
                TeamJoinOutcome.JOINED,
                teamReader.getTeamResponse(joinedTeamId),
                null,
                null);
    }

    /**
     * 팀의 PENDING 가입 신청 목록을 조회한다. 리더만 가능하다.
     *
     * @param requesterId 요청자 ID
     * @param teamId 팀 ID
     * @return PENDING 신청 목록
     */
    public List<TeamJoinApplicationResponse> getPendingJoinApplications(Long requesterId, Long teamId) {
        teamReader.getTeamResponse(teamId);
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, requesterId));
        return teamJoinApplicationReader.getPendingResponses(teamId);
    }

    /**
     * 가입 신청을 승인한다. 리더만 가능하다.
     *
     * @param requesterId 리더 ID
     * @param teamId 팀 ID
     * @param applicationId 신청 ID
     * @return 가입된 팀 정보
     */
    public TeamResponse approveJoinApplication(Long requesterId, Long teamId, Long applicationId) {
        teamReader.getTeamResponse(teamId);
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, requesterId));

        TeamJoinApplicationResponse application =
                teamJoinApplicationReader.getResponse(applicationId, teamId);
        teamMembershipValidator.validateApplicationPending(application.status());
        teamMembershipValidator.validateCanJoin(teamMemberReader.existsByUserId(application.userId()));
        teamMembershipValidator.validateCapacity(teamMemberReader.countMembers(teamId));

        Long approvedTeamId = teamJoinApplicationExecutor.approve(teamId, applicationId, requesterId);
        return teamReader.getTeamResponse(approvedTeamId);
    }

    /**
     * 가입 신청을 거부한다. 리더만 가능하다.
     *
     * @param requesterId 리더 ID
     * @param teamId 팀 ID
     * @param applicationId 신청 ID
     * @return 거부된 신청 정보
     */
    public TeamJoinApplicationResponse rejectJoinApplication(
            Long requesterId,
            Long teamId,
            Long applicationId) {
        teamReader.getTeamResponse(teamId);
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, requesterId));

        TeamJoinApplicationResponse application =
                teamJoinApplicationReader.getResponse(applicationId, teamId);
        teamMembershipValidator.validateApplicationPending(application.status());

        teamJoinApplicationExecutor.reject(teamId, applicationId, requesterId);
        return teamJoinApplicationReader.getResponse(applicationId, teamId);
    }

    /**
     * 본인 가입 신청을 취소한다.
     *
     * @param userId 신청자 ID
     * @param teamId 팀 ID
     * @param applicationId 신청 ID
     * @return 취소된 신청 정보
     */
    public TeamJoinApplicationResponse cancelJoinApplication(
            Long userId,
            Long teamId,
            Long applicationId) {
        teamReader.getTeamResponse(teamId);
        TeamJoinApplicationResponse application =
                teamJoinApplicationReader.getResponse(applicationId, teamId);
        teamMembershipValidator.validateApplicationOwner(application.userId(), userId);
        teamMembershipValidator.validateApplicationPending(application.status());

        teamJoinApplicationExecutor.cancel(teamId, applicationId);
        return teamJoinApplicationReader.getResponse(applicationId, teamId);
    }

    /**
     * 팀 가입 정책을 변경한다. 리더만 가능하다.
     *
     * @param userId 리더 ID
     * @param teamId 팀 ID
     * @param request 정책 변경 요청
     * @return 갱신된 팀 정보
     */
    public TeamResponse updateJoinPolicy(
            Long userId,
            Long teamId,
            UpdateTeamJoinPolicyRequest request) {
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, userId));
        teamMembershipValidator.validateJoinPolicy(request.joinPolicy());
        Long updatedTeamId = teamJoinPolicyExecutor.updateJoinPolicy(teamId, request.joinPolicy());
        return teamReader.getTeamResponse(updatedTeamId);
    }

    /**
     * 팀에서 탈퇴한다. 리더는 위임 후 탈퇴해야 한다.
     *
     * @param userId 탈퇴 유저 ID
     * @param teamId 팀 ID
     */
    public void leave(Long userId, Long teamId) {
        teamMembershipValidator.validateMember(teamMemberReader.isMember(teamId, userId));
        teamMembershipValidator.validateCanLeave(teamMemberReader.isLeader(teamId, userId));
        teamMembershipExecutor.leave(userId, teamId);
    }

    /**
     * 멤버를 강제 탈퇴시킨다. 리더만 가능하다.
     *
     * @param requesterId 요청자(리더) ID
     * @param teamId 팀 ID
     * @param targetUserId 대상 유저 ID
     */
    public void kick(Long requesterId, Long teamId, Long targetUserId) {
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, requesterId));
        teamMembershipValidator.validateKickTarget(requesterId, targetUserId);
        teamMembershipValidator.validateMember(teamMemberReader.isMember(teamId, targetUserId));
        teamMembershipExecutor.kick(teamId, targetUserId);
    }

    /**
     * 팀 멤버 목록을 조회한다.
     *
     * @param teamId 팀 ID
     * @return 멤버 목록
     */
    public List<TeamMemberResponse> getMembers(Long teamId) {
        teamReader.getTeamResponse(teamId);
        return teamMemberReader.getMemberResponses(teamId);
    }

    /**
     * 멤버 계급을 변경한다. 리더만 가능하다.
     *
     * @param requesterId 요청자(리더) ID
     * @param teamId 팀 ID
     * @param targetUserId 대상 유저 ID
     * @param request 계급 변경 요청
     * @return 변경된 멤버 정보
     */
    public TeamMemberResponse updateMemberRole(
            Long requesterId,
            Long teamId,
            Long targetUserId,
            UpdateMemberRoleRequest request) {

        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, requesterId));
        teamMembershipValidator.validateMember(teamMemberReader.isMember(teamId, targetUserId));

        if (teamMemberReader.isLeader(teamId, targetUserId)
                && request.role() == TeamMemberRole.MEMBER) {
            throw new BadRequestException(
                    ErrorCode.INVALID_REQUEST,
                    "리더 강등은 다른 멤버를 LEADER로 지정하여 위임하세요.");
        }

        teamMembershipExecutor.changeRole(teamId, targetUserId, request.role());
        return teamMemberReader.getMemberResponse(teamId, targetUserId);
    }

    /**
     * 팀 재정에 재화를 기부한다.
     *
     * @param userId 기부 유저 ID
     * @param teamId 팀 ID
     * @param request 기부 요청
     * @return 갱신된 재정 정보
     */
    public TeamTreasuryResponse donate(Long userId, Long teamId, DonateTeamRequest request) {
        teamMembershipValidator.validateMember(teamMemberReader.isMember(teamId, userId));
        teamDonateValidator.validateAmount(request.amount());
        teamDonateValidator.validateSufficientCurrency(userReader.getCurrency(userId), request.amount());

        Long donatedTeamId = teamTreasuryExecutor.donate(userId, teamId, request.amount());
        return teamTreasuryReader.getTreasuryResponse(donatedTeamId);
    }

    /**
     * 팀 재정 잔액과 거래 내역을 조회한다.
     *
     * @param teamId 팀 ID
     * @return 재정 정보
     */
    public TeamTreasuryResponse getTreasury(Long teamId) {
        teamReader.getTeamResponse(teamId);
        return teamTreasuryReader.getTreasuryResponse(teamId);
    }

    /**
     * 팀 로고를 변경한다. 리더만 가능하다.
     *
     * @param userId 요청 유저 ID
     * @param teamId 팀 ID
     * @param request 로고 변경 요청
     * @return 갱신된 팀 정보
     */
    public TeamResponse updateLogo(Long userId, Long teamId, UpdateTeamLogoRequest request) {
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, userId));
        Long updatedTeamId = teamLogoExecutor.updateLogo(teamId, request.logoUrl());
        return teamReader.getTeamResponse(updatedTeamId);
    }

    /**
     * 팀 리그 기록을 조회한다.
     *
     * @param teamId 팀 ID
     * @param format 포맷 필터
     * @param tier 티어 필터
     * @param aggregate 합산 여부
     * @return 기록 응답
     */
    public TeamRecordsResponse getRecords(
            Long teamId,
            LeagueFormat format,
            LeagueTier tier,
            boolean aggregate) {
        teamReader.getTeamResponse(teamId);
        return teamRecordReader.getRecords(teamId, format, tier, aggregate);
    }

    /**
     * 팀 멤버 프레즌스 하트비트를 갱신한다.
     *
     * @param userId 유저 ID
     * @param teamId 팀 ID
     */
    public void heartbeat(Long userId, Long teamId) {
        teamMembershipValidator.validateMember(teamMemberReader.isMember(teamId, userId));
        teamPresenceExecutor.heartbeat(teamId, userId);
    }
}
