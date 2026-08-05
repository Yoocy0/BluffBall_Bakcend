package com.project.bluffball.domain.store.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 상점 구매·결제 검증 (원시 값만 사용).
 */
@Component
public class StoreValidator {

    /**
     * 오늘 오퍼에 포함된 구종인지 검증한다.
     *
     * @param offeredToday 오늘 오퍼에 포함 여부
     */
    public void validateOfferedToday(boolean offeredToday) {
        if (!offeredToday) {
            throw new BadRequestException(ErrorCode.STORE_OFFER_NOT_FOUND);
        }
    }

    /**
     * 당일 미구매인지 검증한다.
     *
     * @param purchasedToday 오늘 구매 여부
     */
    public void validateNotPurchasedToday(boolean purchasedToday) {
        if (purchasedToday) {
            throw new ConflictException(ErrorCode.STORE_PITCH_ALREADY_PURCHASED);
        }
    }

    /**
     * 미보유 구종인지 검증한다.
     *
     * @param owned 보유 여부
     */
    public void validateNotOwned(boolean owned) {
        if (owned) {
            throw new ConflictException(ErrorCode.STORE_PITCH_ALREADY_OWNED);
        }
    }

    /**
     * 재화 잔액이 충분한지 검증한다.
     *
     * @param balance 잔액
     * @param price   가격
     */
    public void validateCurrencyEnough(long balance, long price) {
        if (balance < price) {
            throw new BadRequestException(ErrorCode.STORE_CURRENCY_INSUFFICIENT);
        }
    }

    /**
     * 재화 상품이 카탈로그에 있는지 검증한다.
     *
     * @param knownProduct 카탈로그에 존재하면 true
     */
    public void validateKnownCurrencyProduct(boolean knownProduct) {
        if (!knownProduct) {
            throw new BadRequestException(ErrorCode.STORE_PRODUCT_NOT_FOUND);
        }
    }

    /**
     * 결제 토큰 미처리인지 검증한다.
     *
     * @param alreadyProcessed 이미 처리됐으면 true
     */
    public void validatePurchaseNotProcessed(boolean alreadyProcessed) {
        if (alreadyProcessed) {
            throw new ConflictException(ErrorCode.STORE_PURCHASE_ALREADY_PROCESSED);
        }
    }

    /**
     * Play 검증 결과가 유효한지 확인한다.
     *
     * @param valid 유효 여부
     * @param detail 실패 메시지
     */
    public void validatePurchaseValid(boolean valid, String detail) {
        if (!valid) {
            throw new BadRequestException(
                    ErrorCode.STORE_PURCHASE_INVALID,
                    detail == null ? "" : detail);
        }
    }
}
