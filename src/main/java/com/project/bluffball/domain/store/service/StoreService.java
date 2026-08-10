package com.project.bluffball.domain.store.service;

import com.project.bluffball.domain.card.dto.response.UserPitchCardResponse;
import com.project.bluffball.domain.card.service.usecase.reader.UserPitchCardReader;
import com.project.bluffball.domain.store.dto.request.GooglePlayConfirmRequest;
import com.project.bluffball.domain.store.dto.request.StoreEnhancementPurchaseRequest;
import com.project.bluffball.domain.store.dto.request.StorePitchPurchaseRequest;
import com.project.bluffball.domain.store.dto.response.CurrencyProductListResponse;
import com.project.bluffball.domain.store.dto.response.GooglePlayConfirmResponse;
import com.project.bluffball.domain.store.dto.response.StoreCatalogResponse;
import com.project.bluffball.domain.store.dto.response.StoreEnhancementPurchaseResponse;
import com.project.bluffball.domain.store.dto.response.StorePitchPurchaseResponse;
import com.project.bluffball.domain.store.infrastructure.GooglePlayBillingClient;
import com.project.bluffball.domain.store.infrastructure.GooglePlayPurchaseVerification;
import com.project.bluffball.domain.store.service.usecase.executor.GooglePlayCurrencyGrantExecutor;
import com.project.bluffball.domain.store.service.usecase.executor.StoreEnhancementPurchaseExecutor;
import com.project.bluffball.domain.store.service.usecase.executor.StorePitchPurchaseExecutor;
import com.project.bluffball.domain.store.service.usecase.reader.StoreReader;
import com.project.bluffball.domain.store.service.usecase.validator.StoreValidator;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 상시 상점·Google Play 재화 충전 서비스.
 */
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreReader storeReader;
    private final StoreValidator storeValidator;
    private final StorePitchPurchaseExecutor storePitchPurchaseExecutor;
    private final StoreEnhancementPurchaseExecutor storeEnhancementPurchaseExecutor;
    private final GooglePlayCurrencyGrantExecutor googlePlayCurrencyGrantExecutor;
    private final GooglePlayBillingClient googlePlayBillingClient;
    private final UserReader userReader;
    private final UserPitchCardReader userPitchCardReader;

    /**
     * 상시 상점 카탈로그.
     *
     * @param userId 유저 ID
     * @return 카탈로그
     */
    public StoreCatalogResponse getCatalog(Long userId) {
        return storeReader.getCatalog(userId);
    }

    /**
     * 구종 기본본 구매.
     *
     * @param userId  유저 ID
     * @param request 요청
     * @return 구매 결과
     */
    public StorePitchPurchaseResponse purchasePitch(Long userId, StorePitchPurchaseRequest request) {
        Long cardId = request.cardId();
        storeValidator.validateInCatalog(storeReader.isPitchInCatalog(cardId));
        storeValidator.validatePitchBasePurchasable(storeReader.canPurchasePitchBase(userId, cardId));
        long price = storeReader.pitchPrice();
        storeValidator.validateCurrencyEnough(userReader.getCurrency(userId), price);

        Long instanceId = storePitchPurchaseExecutor.purchase(userId, cardId, price);
        UserPitchCardResponse card = userPitchCardReader.getResponseByInstance(userId, instanceId);
        return new StorePitchPurchaseResponse(userReader.getCurrency(userId), card);
    }

    /**
     * 강화 카드 구매.
     *
     * @param userId  유저 ID
     * @param request 요청
     * @return 구매 결과
     */
    public StoreEnhancementPurchaseResponse purchaseEnhancement(
            Long userId, StoreEnhancementPurchaseRequest request) {
        Long enhancementCardId = request.enhancementCardId();
        storeValidator.validateInCatalog(storeReader.isEnhancementInCatalog(enhancementCardId));
        long price = storeReader.enhancementPrice();
        storeValidator.validateCurrencyEnough(userReader.getCurrency(userId), price);

        storeEnhancementPurchaseExecutor.purchase(userId, enhancementCardId, price);
        return new StoreEnhancementPurchaseResponse(
                userReader.getCurrency(userId),
                storeReader.getEnhancementInventoryItem(userId, enhancementCardId));
    }

    /**
     * 재화 상품 목록.
     *
     * @return 상품 목록
     */
    public CurrencyProductListResponse getCurrencyProducts() {
        return storeReader.getCurrencyProducts();
    }

    /**
     * Google Play 결제 확인.
     *
     * @param userId  유저 ID
     * @param request 요청
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
