package com.project.bluffball.domain.store.service.usecase.reader;

import com.project.bluffball.domain.card.entity.PitchCard;
import com.project.bluffball.domain.card.repository.PitchCardRepository;
import com.project.bluffball.domain.card.service.usecase.reader.UserPitchCardReader;
import com.project.bluffball.domain.store.StoreConstants;
import com.project.bluffball.domain.store.dto.response.CurrencyProductListResponse;
import com.project.bluffball.domain.store.dto.response.CurrencyProductResponse;
import com.project.bluffball.domain.store.dto.response.DailyStoreResponse;
import com.project.bluffball.domain.store.dto.response.StorePitchOfferResponse;
import com.project.bluffball.domain.store.entity.StorePitchPurchase;
import com.project.bluffball.domain.store.repository.GooglePlayPurchaseRepository;
import com.project.bluffball.domain.store.repository.StorePitchPurchaseRepository;
import com.project.bluffball.domain.store.service.usecase.DailyStoreOfferGenerator;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import com.project.bluffball.global.config.BillingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 상점 오퍼·상품·구매 상태 읽기 Reader.
 */
@Component
@RequiredArgsConstructor
public class StoreReader {

    /** 유저 Reader */
    private final UserReader userReader;

    /** 마스터 구종 Repository */
    private final PitchCardRepository pitchCardRepository;

    /** 보유 구종 Reader */
    private final UserPitchCardReader userPitchCardReader;

    /** 일일 오퍼 생성기 */
    private final DailyStoreOfferGenerator dailyStoreOfferGenerator;

    /** 구종 구매 기록 */
    private final StorePitchPurchaseRepository storePitchPurchaseRepository;

    /** Google 결제 기록 */
    private final GooglePlayPurchaseRepository googlePlayPurchaseRepository;

    /** Billing 설정 */
    private final BillingProperties billingProperties;

    /**
     * 오늘(KST) 일일 상점 상태를 반환한다.
     *
     * @param userId 유저 ID
     * @return 일일 상점
     */
    public DailyStoreResponse getDailyStore(Long userId) {
        LocalDate offerDate = LocalDate.now(StoreConstants.STORE_ZONE);
        List<PitchCard> offers = resolveTodayOffers(userId, offerDate);
        Set<Long> purchasedIds = storePitchPurchaseRepository
                .findByUserIdAndOfferDateAndCardIdIn(
                        userId,
                        offerDate,
                        offers.stream().map(PitchCard::getId).toList())
                .stream()
                .map(StorePitchPurchase::getCardId)
                .collect(Collectors.toCollection(HashSet::new));

        long currency = userReader.getCurrency(userId);
        List<StorePitchOfferResponse> offerResponses = offers.stream()
                .map(card -> toOfferResponse(userId, card, purchasedIds.contains(card.getId())))
                .toList();
        return new DailyStoreResponse(offerDate, currency, offerResponses);
    }

    /**
     * 오늘 오퍼에 해당 구종이 포함되는지.
     *
     * @param userId 유저 ID
     * @param cardId 구종 ID
     * @return 포함되면 true
     */
    public boolean isOfferedToday(Long userId, Long cardId) {
        LocalDate offerDate = LocalDate.now(StoreConstants.STORE_ZONE);
        return resolveTodayOffers(userId, offerDate).stream()
                .anyMatch(card -> card.getId().equals(cardId));
    }

    /**
     * 오늘 해당 구종 구매 여부.
     *
     * @param userId 유저 ID
     * @param cardId 구종 ID
     * @return 구매했으면 true
     */
    public boolean isPurchasedToday(Long userId, Long cardId) {
        LocalDate offerDate = LocalDate.now(StoreConstants.STORE_ZONE);
        return storePitchPurchaseRepository.existsByUserIdAndOfferDateAndCardId(userId, offerDate, cardId);
    }

    /**
     * 오늘 오퍼 날짜(KST).
     *
     * @return 날짜
     */
    public LocalDate todayOfferDate() {
        return LocalDate.now(StoreConstants.STORE_ZONE);
    }

    /**
     * 구종 기본 가격.
     *
     * @return 가격
     */
    public long pitchPrice() {
        return StoreConstants.DEFAULT_PITCH_PRICE;
    }

    /**
     * 재화 상품 목록.
     *
     * @return 상품 목록
     */
    public CurrencyProductListResponse getCurrencyProducts() {
        List<CurrencyProductResponse> products = billingProperties.getCurrencyProducts().stream()
                .map(p -> new CurrencyProductResponse(p.getProductId(), p.getCurrencyAmount()))
                .toList();
        return new CurrencyProductListResponse(products);
    }

    /**
     * 재화 상품 지급량을 조회한다.
     *
     * @param productId SKU
     * @return 지급량 Optional
     */
    public Optional<Long> findCurrencyAmount(String productId) {
        return billingProperties.getCurrencyProducts().stream()
                .filter(p -> productId.equals(p.getProductId()))
                .map(BillingProperties.CurrencyProduct::getCurrencyAmount)
                .findFirst();
    }

    /**
     * purchaseToken 처리 여부.
     *
     * @param purchaseToken 토큰
     * @return 처리됐으면 true
     */
    public boolean isPurchaseTokenProcessed(String purchaseToken) {
        return googlePlayPurchaseRepository.existsByPurchaseToken(purchaseToken);
    }

    /**
     * orderId 처리 여부.
     *
     * @param orderId 주문 ID
     * @return 처리됐으면 true
     */
    public boolean isOrderIdProcessed(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return false;
        }
        return googlePlayPurchaseRepository.existsByOrderId(orderId);
    }

    private List<PitchCard> resolveTodayOffers(Long userId, LocalDate offerDate) {
        List<PitchCard> catalog = pitchCardRepository.findAll();
        return dailyStoreOfferGenerator.generate(userId, offerDate.toEpochDay(), catalog);
    }

    private StorePitchOfferResponse toOfferResponse(Long userId, PitchCard card, boolean purchasedToday) {
        boolean owned = userPitchCardReader.exists(userId, card.getId());
        boolean purchasable = !owned && !purchasedToday;
        return new StorePitchOfferResponse(
                card.getId(),
                card.getName(),
                StoreConstants.DEFAULT_PITCH_PRICE,
                owned,
                purchasedToday,
                purchasable);
    }
}
