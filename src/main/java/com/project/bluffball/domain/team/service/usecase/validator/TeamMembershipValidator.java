package com.project.bluffball.domain.team.service.usecase.validator;

import com.project.bluffball.domain.team.enums.TeamJoinApplicationStatus;
import com.project.bluffball.domain.team.enums.TeamJoinPolicy;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.ForbiddenException;
import org.springframework.stereotype.Component;

/**
 * 팀 멤버십·권한 규칙 검증 (usecase/validator 계층).
 */
@Component
public class TeamMembershipValidator {

    /** 팀 최대 인원 (Team.MAX_MEMBERS와 동일) */
    private static final int MAX_MEMBERS = 50;

    /**
     * 팀 멤버인지 검증한다.
     *
     * @param isMember 멤버 여부
     * @throws ForbiddenException 멤버가 아니면
     */
    public void validateMember(boolean isMember) {
        if (!isMember) {
            throw new ForbiddenException(ErrorCode.TEAM_NOT_MEMBER);
        }
    }

    /**
     * 팀 리더인지 검증한다.
     *
     * @param isLeader 리더 여부
     * @throws ForbiddenException 리더가 아니면
     */
    public void validateLeader(boolean isLeader) {
        if (!isLeader) {
            throw new ForbiddenException(ErrorCode.TEAM_FORBIDDEN);
        }
    }

    /**
     * 미소속 상태에서만 가입 가능함을 검증한다.
     *
     * @param alreadyJoined 이미 소속 여부
     * @throws ConflictException 이미 소속이면
     */
    public void validateCanJoin(boolean alreadyJoined) {
        if (alreadyJoined) {
            throw new ConflictException(ErrorCode.TEAM_ALREADY_JOINED);
        }
    }

    /**
     * 팀 최대 인원 초과 여부를 검증한다.
     *
     * @param memberCount 현재 멤버 수
     * @throws ConflictException 정원이 가득 찼으면
     */
    public void validateCapacity(long memberCount) {
        if (memberCount >= MAX_MEMBERS) {
            throw new ConflictException(
                    ErrorCode.TEAM_MEMBER_LIMIT_EXCEEDED,
                    "memberCount=" + memberCount + ", max=" + MAX_MEMBERS);
        }
    }

    /**
     * 리더는 탈퇴할 수 없음을 검증한다.
     *
     * @param isLeader 리더 여부
     * @throws ConflictException 리더이면
     */
    public void validateCanLeave(boolean isLeader) {
        if (isLeader) {
            throw new ConflictException(ErrorCode.TEAM_LEADER_CANNOT_LEAVE);
        }
    }

    /**
     * 리더 자신은 강제 탈퇴 대상이 될 수 없음을 검증한다.
     *
     * @param requesterId 요청자 ID
     * @param targetUserId 대상 유저 ID
     * @throws BadRequestException 자기 자신 킥이면
     */
    public void validateKickTarget(Long requesterId, Long targetUserId) {
        if (requesterId.equals(targetUserId)) {
            throw new BadRequestException(ErrorCode.TEAM_LEADER_CANNOT_KICK_SELF);
        }
    }

    /**
     * 가입 정책 값이 유효한지 검증한다.
     *
     * @param joinPolicy 가입 정책
     * @throws BadRequestException null이면
     */
    public void validateJoinPolicy(TeamJoinPolicy joinPolicy) {
        if (joinPolicy == null) {
            throw new BadRequestException(ErrorCode.TEAM_JOIN_POLICY_INVALID);
        }
    }

    /**
     * 동일 팀에 대한 PENDING 신청이 없는지 검증한다.
     *
     * @param alreadyPending 이미 PENDING 여부
     * @throws ConflictException 이미 있으면
     */
    public void validateNoPendingApplication(boolean alreadyPending) {
        if (alreadyPending) {
            throw new ConflictException(ErrorCode.TEAM_JOIN_APPLICATION_ALREADY_PENDING);
        }
    }

    /**
     * 신청 상태가 PENDING인지 검증한다.
     *
     * @param status 신청 상태
     * @throws BadRequestException PENDING이 아니면
     */
    public void validateApplicationPending(TeamJoinApplicationStatus status) {
        if (status != TeamJoinApplicationStatus.PENDING) {
            throw new BadRequestException(
                    ErrorCode.TEAM_JOIN_APPLICATION_NOT_PENDING, "status=" + status);
        }
    }

    /**
     * 신청자 본인만 취소할 수 있음을 검증한다.
     *
     * @param applicantUserId 신청자 ID
     * @param requesterUserId 요청자 ID
     * @throws ForbiddenException 본인이 아니면
     */
    public void validateApplicationOwner(Long applicantUserId, Long requesterUserId) {
        if (!applicantUserId.equals(requesterUserId)) {
            throw new ForbiddenException(ErrorCode.TEAM_FORBIDDEN);
        }
    }
}
