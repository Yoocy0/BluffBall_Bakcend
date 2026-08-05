package com.project.bluffball.domain.card.service.usecase.validator;

import com.project.bluffball.domain.card.enums.TimingEnhancement;
import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 유저 구종 카드 강화·보유 규칙 검증 (usecase/validator 계층).
 */
@Component
public class UserPitchCardValidator {

    /**
     * 미보유일 때만 획득 가능함을 검증한다.
     *
     * @param alreadyOwned 이미 보유 여부
     */
    public void validateNotOwned(boolean alreadyOwned) {
        if (alreadyOwned) {
            throw new ConflictException(ErrorCode.USER_PITCH_CARD_ALREADY_OWNED);
        }
    }

    /**
     * 변화량 강화가 아직 없음을 검증한다.
     *
     * @param alreadyEnhanced 이미 강화됨
     */
    public void validateChangeAmountNotEnhanced(boolean alreadyEnhanced) {
        if (alreadyEnhanced) {
            throw new ConflictException(ErrorCode.USER_PITCH_CARD_ALREADY_ENHANCED, "changeAmount");
        }
    }

    /**
     * 타이밍 강화가 아직 없음을 검증한다.
     *
     * @param current 현재 타이밍 강화 상태
     */
    public void validateTimingNotEnhanced(TimingEnhancement current) {
        if (current != TimingEnhancement.NONE) {
            throw new ConflictException(ErrorCode.USER_PITCH_CARD_ALREADY_ENHANCED, "timing");
        }
    }

    /**
     * 타이밍 강화 방향이 FASTER/SLOWER인지 검증한다.
     *
     * @param enhancement 요청 강화
     */
    public void validateTimingEnhancementDirection(TimingEnhancement enhancement) {
        if (enhancement == null || enhancement == TimingEnhancement.NONE) {
            throw new BadRequestException(ErrorCode.USER_PITCH_CARD_TIMING_INVALID);
        }
    }

    /**
     * 타이밍 강화가 마스터 타이밍 경계를 넘지 않는지 검증한다.
     *
     * @param baseTiming 마스터 타이밍
     * @param enhancement FASTER 또는 SLOWER
     */
    public void validateTimingBoundary(Timing baseTiming, TimingEnhancement enhancement) {
        int ordinal = baseTiming.ordinal();
        if (enhancement == TimingEnhancement.FASTER && ordinal <= Timing.TOO_EARLY.ordinal()) {
            throw new BadRequestException(
                    ErrorCode.USER_PITCH_CARD_TIMING_BOUNDARY,
                    "base=" + baseTiming + ", enhancement=" + enhancement);
        }
        if (enhancement == TimingEnhancement.SLOWER && ordinal >= Timing.TOO_LATE.ordinal()) {
            throw new BadRequestException(
                    ErrorCode.USER_PITCH_CARD_TIMING_BOUNDARY,
                    "base=" + baseTiming + ", enhancement=" + enhancement);
        }
    }
}
