package com.project.bluffball.domain.card.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 강화 카드 보유·소모 검증 (원시 값만).
 */
@Component
public class UserEnhancementCardValidator {

    /**
     * 보유 수량이 충분한지 검증한다.
     *
     * @param quantity 보유 수량
     * @param required 필요 수량
     */
    public void validateSufficientQuantity(int quantity, int required) {
        if (quantity < required) {
            throw new BadRequestException(ErrorCode.USER_ENHANCEMENT_CARD_INSUFFICIENT);
        }
    }
}
