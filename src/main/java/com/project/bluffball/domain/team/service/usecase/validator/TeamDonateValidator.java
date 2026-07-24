package com.project.bluffball.domain.team.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 팀 재화 기부 요청 검증 (usecase/validator 계층).
 */
@Component
public class TeamDonateValidator {

    /**
     * 기부 금액이 양수인지 검증한다.
     *
     * @param amount 기부 금액
     * @throws BadRequestException 금액이 0 이하이면
     */
    public void validateAmount(long amount) {
        if (amount <= 0) {
            throw new BadRequestException(
                    ErrorCode.TEAM_DONATE_AMOUNT_INVALID,
                    "amount=" + amount);
        }
    }

    /**
     * 유저 재화 잔액이 기부 금액 이상인지 검증한다.
     *
     * @param currency 보유 재화
     * @param amount 기부 금액
     * @throws BadRequestException 잔액 부족 시
     */
    public void validateSufficientCurrency(long currency, long amount) {
        if (currency < amount) {
            throw new BadRequestException(
                    ErrorCode.TEAM_CURRENCY_INSUFFICIENT,
                    "currency=" + currency + ", amount=" + amount);
        }
    }
}
