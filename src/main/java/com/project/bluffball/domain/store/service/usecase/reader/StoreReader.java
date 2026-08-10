package com.project.bluffball.domain.store.service.usecase.reader;

import com.project.bluffball.domain.card.entity.EnhancementCard;
import com.project.bluffball.domain.card.entity.PitchCard;
import com.project.bluffball.domain.card.repository.EnhancementCardRepository;
import com.project.bluffball.domain.card.repository.PitchCardRepository;
import com.project.bluffball.domain.card.service.usecase.reader.EnhancementCardReader;
import com.project.bluffball.domain.card.service.usecase.reader.UserPitchCardReader;
import com.project.bluffball.domain.store.StoreConstants;
import com.project.bluffball.domain.store.dto.response.CurrencyProductListResponse;
import com.project.bluffball.domain.store.dto.response.CurrencyProductResponse;
import com.project.bluffball.domain.store.dto.response.StoreCatalogResponse;
import com.project.bluffball.domain.store.dto.response.StoreEnhancementOfferResponse;
import com.project.bluffball.domain.store.dto.response.StorePitchOfferResponse;
import com.project.bluffball.domain.store.repository.GooglePlayPurchaseRepository;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import com.project.bluffball.global.config.BillingProperties;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 상점 카탈로그·결제 상태 Reader.
 */
@Component
@RequiredArgsConstructor
public class StoreReader {

    private final UserReader userReader;
    private final PitchCardRepository pitchCardRepository;
    private final EnhancementCardRepository enhancementCardRepository;
    private final UserPitchCardReader userPitchCardReader;
    private final EnhancementCardReader enhancementCardReader;
    private final GooglePlayPurchaseRepository googlePlayPurchaseRepository;
    private final BillingProperties billingProperties;

    /**
     * 상시 상점 카탈로그.
     *
     * @param userId 유저 ID
     * @return 카탈로그
     */
    public StoreCatalogResponse getCatalog(Long userId) {
        long currency = userReader.getCurrency(userId);
        List<StorePitchOfferResponse> pitches = pitchCardRepository.findAll().stream()
                .sorted(Comparator.comparing(PitchCard::getName))
                .map(card -> toPitchOffer(userId, card))
                .toList();
        List<StoreEnhancementOfferResponse> enhancements = enhancementCardRepository.findAll().stream()
                .sorted(Comparator.comparing(c -> c.getEffect().ordinal()))
                .map(card -> toEnhancementOffer(userId, card))
                .toList();
        return new StoreCatalogResponse(currency, pitches, enhancements);
    }

    /**
     * 마스터 구종이 상점에 있는지.
     *
     * @param cardId 마스터 ID
     * @return 존재하면 true
     */
    public boolean isPitchInCatalog(Long cardId) {
        return pitchCardRepository.existsById(cardId);
    }

    /**
     * 강화 카드가 상점에 있는지.
     *
     * @param enhancementCardId 강화 마스터 ID
     * @return 존재하면 true
     */
    public boolean isEnhancementInCatalog(Long enhancementCardId) {
        return enhancementCardRepository.existsById(enhancementCardId);
    }

    /**
     * 구종 기본본 구매 가능 여부 (미강화 인스턴스 없음).
     *
     * @param userId 유저 ID
     * @param cardId 마스터 ID
     * @return 구매 가능하면 true
     */
    public boolean canPurchasePitchBase(Long userId, Long cardId) {
        return !userPitchCardReader.hasUnenhancedInstance(userId, cardId);
    }

    public long pitchPrice() {
        return StoreConstants.DEFAULT_PITCH_PRICE;
    }

    public long enhancementPrice() {
        return StoreConstants.DEFAULT_ENHANCEMENT_PRICE;
    }

    public CurrencyProductListResponse getCurrencyProducts() {
        List<CurrencyProductResponse> products = billingProperties.getCurrencyProducts().stream()
                .map(p -> new CurrencyProductResponse(p.getProductId(), p.getCurrencyAmount()))
                .toList();
        return new CurrencyProductListResponse(products);
    }

    public Optional<Long> findCurrencyAmount(String productId) {
        return billingProperties.getCurrencyProducts().stream()
                .filter(p -> productId.equals(p.getProductId()))
                .map(BillingProperties.CurrencyProduct::getCurrencyAmount)
                .findFirst();
    }

    public boolean isPurchaseTokenProcessed(String purchaseToken) {
        return googlePlayPurchaseRepository.existsByPurchaseToken(purchaseToken);
    }

    public boolean isOrderIdProcessed(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return false;
        }
        return googlePlayPurchaseRepository.existsByOrderId(orderId);
    }

    /**
     * 강화 카드 인벤 한 건.
     *
     * @param userId 유저 ID
     * @param cardId 강화 마스터 ID
     * @return 응답
     */
    public com.project.bluffball.domain.card.dto.response.UserEnhancementCardResponse getEnhancementInventoryItem(
            Long userId, Long cardId) {
        return enhancementCardReader.getInventoryResponses(userId).stream()
                .filter(r -> r.cardId().equals(cardId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.ENHANCEMENT_CARD_NOT_FOUND, "cardId=" + cardId));
    }

    private StorePitchOfferResponse toPitchOffer(Long userId, PitchCard card) {
        boolean hasBase = userPitchCardReader.hasUnenhancedInstance(userId, card.getId());
        return new StorePitchOfferResponse(
                card.getId(),
                card.getName(),
                StoreConstants.DEFAULT_PITCH_PRICE,
                hasBase,
                !hasBase);
    }

    private StoreEnhancementOfferResponse toEnhancementOffer(Long userId, EnhancementCard card) {
        return new StoreEnhancementOfferResponse(
                card.getId(),
                card.getName(),
                card.getEffect(),
                StoreConstants.DEFAULT_ENHANCEMENT_PRICE,
                enhancementCardReader.getQuantity(userId, card.getId()));
    }
}
