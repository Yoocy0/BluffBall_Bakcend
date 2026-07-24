package com.project.bluffball.domain.team.service.usecase.validator;

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
}
