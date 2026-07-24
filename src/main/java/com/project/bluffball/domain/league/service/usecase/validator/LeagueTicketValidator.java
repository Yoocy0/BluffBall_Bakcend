package com.project.bluffball.domain.league.service.usecase.validator;

import com.project.bluffball.domain.league.enums.LeagueSeasonStatus;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 리그 참여권 구매 규칙 검증 (usecase/validator 계층).
 */
@Component
public class LeagueTicketValidator {

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
     * 이미 참여권을 보유하지 않았는지 검증한다.
     *
     * @param alreadyOwned 보유 여부
     * @throws ConflictException 이미 보유하면
     */
    public void validateNotAlreadyOwned(boolean alreadyOwned) {
        if (alreadyOwned) {
            throw new ConflictException(ErrorCode.LEAGUE_TICKET_ALREADY_OWNED);
        }
    }

    /**
     * 팀 재정이 참여권 비용 이상인지 검증한다.
     *
     * @param treasury 팀 재정
     * @param entryFee 참여권 비용
     * @throws BadRequestException 재정 부족 시
     */
    public void validateSufficientTreasury(long treasury, long entryFee) {
        if (treasury < entryFee) {
            throw new BadRequestException(
                    ErrorCode.LEAGUE_TREASURY_INSUFFICIENT,
                    "treasury=" + treasury + ", entryFee=" + entryFee);
        }
    }
}
