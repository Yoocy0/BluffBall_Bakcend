package com.project.bluffball.domain.store.service.usecase.executor;

import com.project.bluffball.domain.store.entity.GooglePlayPurchase;
import com.project.bluffball.domain.store.repository.GooglePlayPurchaseRepository;
import com.project.bluffball.domain.user.entity.User;
import com.project.bluffball.domain.user.repository.UserRepository;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Google Play 결제 확인 후 재화 지급 Executor.
 */
@Component
@RequiredArgsConstructor
public class GooglePlayCurrencyGrantExecutor {

    /** 유저 Reader */
    private final UserReader userReader;

    /** 유저 Repository */
    private final UserRepository userRepository;

    /** 결제 기록 Repository */
    private final GooglePlayPurchaseRepository googlePlayPurchaseRepository;

    /**
     * 재화를 지급하고 결제 처리를 기록한다.
     *
     * @param userId         유저 ID
     * @param productId      상품 ID
     * @param purchaseToken  구매 토큰
     * @param orderId        주문 ID (없으면 null)
     * @param currencyAmount 지급량
     * @return 지급량
     */
    @Transactional
    public long grant(
            Long userId,
            String productId,
            String purchaseToken,
            String orderId,
            long currencyAmount) {
        User user = userReader.getById(userId);
        user.addCurrency(currencyAmount);
        userRepository.save(user);
        googlePlayPurchaseRepository.save(
                new GooglePlayPurchase(userId, productId, purchaseToken, orderId, currencyAmount));
        return currencyAmount;
    }
}
