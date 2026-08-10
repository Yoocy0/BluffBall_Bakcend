package com.project.bluffball.domain.store.service.usecase.executor;

import com.project.bluffball.domain.card.service.usecase.executor.UserEnhancementCardExecutor;
import com.project.bluffball.domain.user.entity.User;
import com.project.bluffball.domain.user.repository.UserRepository;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상점 강화 카드 구매 Executor.
 */
@Component
@RequiredArgsConstructor
public class StoreEnhancementPurchaseExecutor {

    /** 유저 Reader */
    private final UserReader userReader;

    /** 유저 Repository */
    private final UserRepository userRepository;

    /** 강화 카드 지급 Executor */
    private final UserEnhancementCardExecutor userEnhancementCardExecutor;

    /**
     * 재화를 차감하고 강화 카드 1장을 지급한다.
     *
     * @param userId            유저 ID
     * @param enhancementCardId 강화 카드 마스터 ID
     * @param price             가격
     * @return 강화 카드 마스터 ID
     */
    @Transactional
    public Long purchase(Long userId, Long enhancementCardId, long price) {
        User user = userReader.getById(userId);
        try {
            user.spendCurrency(price);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ErrorCode.STORE_CURRENCY_INSUFFICIENT, ex.getMessage());
        }
        userRepository.save(user);
        userEnhancementCardExecutor.grant(userId, enhancementCardId, 1);
        return enhancementCardId;
    }
}
