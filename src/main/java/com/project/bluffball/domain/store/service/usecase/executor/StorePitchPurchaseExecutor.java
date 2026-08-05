package com.project.bluffball.domain.store.service.usecase.executor;

import com.project.bluffball.domain.card.service.usecase.executor.UserPitchCardExecutor;
import com.project.bluffball.domain.store.entity.StorePitchPurchase;
import com.project.bluffball.domain.store.repository.StorePitchPurchaseRepository;
import com.project.bluffball.domain.user.entity.User;
import com.project.bluffball.domain.user.repository.UserRepository;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 일일 상점 구종 구매 Executor.
 */
@Component
@RequiredArgsConstructor
public class StorePitchPurchaseExecutor {

    /** 유저 Reader */
    private final UserReader userReader;

    /** 유저 Repository */
    private final UserRepository userRepository;

    /** 구종 획득 Executor */
    private final UserPitchCardExecutor userPitchCardExecutor;

    /** 구매 기록 Repository */
    private final StorePitchPurchaseRepository storePitchPurchaseRepository;

    /**
     * 재화를 차감하고 구종을 지급·구매 기록을 남긴다.
     *
     * @param userId    유저 ID
     * @param offerDate 오퍼 날짜
     * @param cardId    구종 ID
     * @param price     가격
     * @return 구매한 cardId
     */
    @Transactional
    public Long purchase(Long userId, LocalDate offerDate, Long cardId, long price) {
        User user = userReader.getById(userId);
        try {
            user.spendCurrency(price);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ErrorCode.STORE_CURRENCY_INSUFFICIENT, ex.getMessage());
        }
        userRepository.save(user);
        userPitchCardExecutor.acquireIfMissing(userId, List.of(cardId));
        storePitchPurchaseRepository.save(new StorePitchPurchase(userId, offerDate, cardId, price));
        return cardId;
    }
}
