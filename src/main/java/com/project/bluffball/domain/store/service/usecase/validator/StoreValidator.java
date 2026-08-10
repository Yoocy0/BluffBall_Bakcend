package com.project.bluffball.domain.store.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 상점 구매·결제 검증.
 */
@Component
public class StoreValidator {

    /**
     * 카탈로그에 있는 상품인지.
     *
     * @param inCatalog 카탈로그 포함 여부
     */
    public void validateInCatalog(boolean inCatalog) {
        if (!inCatalog) {
            throw new BadRequestException(ErrorCode.STORE_OFFER_NOT_FOUND);
        }
    }

    /**
     * 구종 기본본 구매 가능 여부 (이미 기본본 있으면 거부).
     *
     * @param canPurchase 구매 가능
     */
    public void validatePitchBasePurchasable(boolean canPurchase) {
        if (!canPurchase) {
            throw new ConflictException(ErrorCode.STORE_PITCH_ALREADY_OWNED, "unenhanced base already owned");
        }
    }

    /**
     * 재화 잔액.
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
     * 재화 상품 존재.
     *
     * @param knownProduct 존재 여부
     */
    public void validateKnownCurrencyProduct(boolean knownProduct) {
        if (!knownProduct) {
            throw new BadRequestException(ErrorCode.STORE_PRODUCT_NOT_FOUND);
        }
    }

    /**
     * 결제 미처리.
     *
     * @param alreadyProcessed 이미 처리됨
     */
    public void validatePurchaseNotProcessed(boolean alreadyProcessed) {
        if (alreadyProcessed) {
            throw new ConflictException(ErrorCode.STORE_PURCHASE_ALREADY_PROCESSED);
        }
    }

    /**
     * Play 검증 유효.
     *
     * @param valid  유효
     * @param detail 메시지
     */
    public void validatePurchaseValid(boolean valid, String detail) {
        if (!valid) {
            throw new BadRequestException(
                    ErrorCode.STORE_PURCHASE_INVALID,
                    detail == null ? "" : detail);
        }
    }
}
