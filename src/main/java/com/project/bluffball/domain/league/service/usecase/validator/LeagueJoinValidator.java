package com.project.bluffball.domain.league.service.usecase.validator;

import com.project.bluffball.domain.league.enums.LeagueSeasonStatus;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 리그 시즌 참가 규칙 검증 (usecase/validator 계층).
 */
@Component
public class LeagueJoinValidator {

    /**
     * 시즌이 모집 중인지 검증한다.
     *
     * @param status 시즌 상태
     * @throws BadRequestException 모집 중이 아니면
     */
    public void validateRecruiting(LeagueSeasonStatus status) {
        if (status != LeagueSeasonStatus.RECRUITING) {
            throw new BadRequestException(ErrorCode.LEAGUE_NOT_RECRUITING, "status=" + status);
        }
    }

    /**
     * 참여권 보유 여부를 검증한다.
     *
     * @param hasTicket 참여권 보유 여부
     * @throws BadRequestException 참여권이 없으면
     */
    public void validateTicketOwned(boolean hasTicket) {
        if (!hasTicket) {
            throw new BadRequestException(ErrorCode.LEAGUE_TICKET_NOT_FOUND);
        }
    }

    /**
     * 최소 팀원 수를 검증한다.
     *
     * @param memberCount 현재 팀원 수
     * @param minTeamMembers 최소 필요 인원
     * @throws BadRequestException 인원 부족 시
     */
    public void validateMemberCount(long memberCount, int minTeamMembers) {
        if (memberCount < minTeamMembers) {
            throw new BadRequestException(
                    ErrorCode.LEAGUE_MEMBERS_INSUFFICIENT,
                    "memberCount=" + memberCount + ", min=" + minTeamMembers);
        }
    }

    /**
     * 이미 시즌에 참가하지 않았는지 검증한다.
     *
     * @param alreadyJoined 참가 여부
     * @throws ConflictException 이미 참가 중이면
     */
    public void validateNotAlreadyJoined(boolean alreadyJoined) {
        if (alreadyJoined) {
            throw new ConflictException(ErrorCode.LEAGUE_ALREADY_JOINED);
        }
    }

    /**
     * 시즌 정원이 남아 있는지 검증한다.
     *
     * @param joinedTeamCount 현재 참가 팀 수
     * @param maxTeams 최대 팀 수
     * @throws ConflictException 정원 초과 시
     */
    public void validateCapacity(long joinedTeamCount, int maxTeams) {
        if (joinedTeamCount >= maxTeams) {
            throw new ConflictException(
                    ErrorCode.LEAGUE_SEASON_FULL,
                    "joined=" + joinedTeamCount + ", max=" + maxTeams);
        }
    }
}
