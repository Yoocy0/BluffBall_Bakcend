package com.project.bluffball.domain.store.service;

import com.project.bluffball.domain.card.dto.response.UserPitchCardResponse;
import com.project.bluffball.domain.card.service.usecase.reader.UserPitchCardReader;
import com.project.bluffball.domain.store.dto.request.GooglePlayConfirmRequest;
import com.project.bluffball.domain.store.dto.request.StorePitchPurchaseRequest;
import com.project.bluffball.domain.store.dto.response.CurrencyProductListResponse;
import com.project.bluffball.domain.store.dto.response.DailyStoreResponse;
import com.project.bluffball.domain.store.dto.response.GooglePlayConfirmResponse;
import com.project.bluffball.domain.store.dto.response.StorePitchPurchaseResponse;
import com.project.bluffball.domain.store.infrastructure.GooglePlayBillingClient;
import com.project.bluffball.domain.store.infrastructure.GooglePlayPurchaseVerification;
import com.project.bluffball.domain.store.service.usecase.executor.GooglePlayCurrencyGrantExecutor;
import com.project.bluffball.domain.store.service.usecase.executor.StorePitchPurchaseExecutor;
import com.project.bluffball.domain.store.service.usecase.reader.StoreReader;
import com.project.bluffball.domain.store.service.usecase.validator.StoreValidator;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 일일 상점·Google Play 재화 충전 서비스.
 */
@Service
@RequiredArgsConstructor
public class StoreService {

    /** 상점 Reader */
    private final StoreReader storeReader;

    /** 상점 검증 */
    private final StoreValidator storeValidator;

    /** 구종 구매 Executor */
    private final StorePitchPurchaseExecutor storePitchPurchaseExecutor;

    /** 재화 지급 Executor */
    private final GooglePlayCurrencyGrantExecutor googlePlayCurrencyGrantExecutor;

    /** Play 결제 검증 클라이언트 */
    private final GooglePlayBillingClient googlePlayBillingClient;

    /** 유저 Reader */
    private final UserReader userReader;

    /** 보유 구종 Reader */
    private final UserPitchCardReader userPitchCardReader;

    /**
     * 오늘 일일 상점을 조회한다.
     *
     * @param userId 유저 ID
     * @return 일일 상점
     */
    public DailyStoreResponse getDailyStore(Long userId) {
        return storeReader.getDailyStore(userId);
    }

    /**
     * 오늘 오퍼 구종을 재화로 구매한다.
     *
     * @param userId  유저 ID
     * @param request 구매 요청
     * @return 구매 결과
     */
    public StorePitchPurchaseResponse purchasePitch(Long userId, StorePitchPurchaseRequest request) {
        Long cardId = request.cardId();
        storeValidator.validateOfferedToday(storeReader.isOfferedToday(userId, cardId));
        storeValidator.validateNotPurchasedToday(storeReader.isPurchasedToday(userId, cardId));
        storeValidator.validateNotOwned(userPitchCardReader.exists(userId, cardId));

        long price = storeReader.pitchPrice();
        storeValidator.validateCurrencyEnough(userReader.getCurrency(userId), price);

        Long purchasedId = storePitchPurchaseExecutor.purchase(
                userId, storeReader.todayOfferDate(), cardId, price);

        UserPitchCardResponse card = userPitchCardReader.getResponse(userId, purchasedId);
        return new StorePitchPurchaseResponse(userReader.getCurrency(userId), card);
    }

    /**
     * 재화 인앱 상품 목록을 조회한다.
     *
     * @return 상품 목록
     */
    public CurrencyProductListResponse getCurrencyProducts() {
        return storeReader.getCurrencyProducts();
    }

    /**
     * Google Play 결제를 검증하고 재화를 지급한다.
     *
     * @param userId  유저 ID
     * @param request 결제 확인 요청
     * @return 지급 결과
     */
    public GooglePlayConfirmResponse confirmGooglePlayPurchase(
            Long userId, GooglePlayConfirmRequest request) {
        String productId = request.productId();
        String purchaseToken = request.purchaseToken();

        var amountOpt = storeReader.findCurrencyAmount(productId);
        storeValidator.validateKnownCurrencyProduct(amountOpt.isPresent());
        long currencyAmount = amountOpt.orElseThrow();

        storeValidator.validatePurchaseNotProcessed(storeReader.isPurchaseTokenProcessed(purchaseToken));

        GooglePlayPurchaseVerification verification =
                googlePlayBillingClient.verifyProductPurchase(productId, purchaseToken);
        storeValidator.validatePurchaseValid(verification.valid(), verification.message());
        storeValidator.validatePurchaseNotProcessed(storeReader.isOrderIdProcessed(verification.orderId()));

        googlePlayCurrencyGrantExecutor.grant(
                userId, productId, purchaseToken, verification.orderId(), currencyAmount);
        googlePlayBillingClient.acknowledgeProductPurchase(productId, purchaseToken);

        return new GooglePlayConfirmResponse(
                productId, currencyAmount, userReader.getCurrency(userId));
    }
}
