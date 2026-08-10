package com.project.bluffball.domain.card.service.usecase.executor;

import com.project.bluffball.domain.card.entity.UserEnhancementCard;
import com.project.bluffball.domain.card.repository.EnhancementCardRepository;
import com.project.bluffball.domain.card.repository.UserEnhancementCardRepository;
import com.project.bluffball.domain.card.service.usecase.reader.EnhancementCardReader;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유저 강화 카드 획득·소모 Executor.
 */
@Component
@RequiredArgsConstructor
public class UserEnhancementCardExecutor {

    /** 유저 보유 Repository */
    private final UserEnhancementCardRepository userEnhancementCardRepository;

    /** 마스터 Repository */
    private final EnhancementCardRepository enhancementCardRepository;

    /** 강화 카드 Reader */
    private final EnhancementCardReader enhancementCardReader;

    /**
     * 강화 카드를 수량만큼 획득한다.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 ID
     * @param amount 획득 수량 (1 이상)
     * @return 마스터 ID
     */
    @Transactional
    public Long grant(Long userId, Long cardId, int amount) {
        requireMasterExists(cardId);
        if (amount < 1) {
            throw new BadRequestException(ErrorCode.INVALID_REQUEST, "amount must be >= 1");
        }
        userEnhancementCardRepository.findByUserIdAndEnhancementCardId(userId, cardId)
                .ifPresentOrElse(
                        owned -> owned.addQuantity(amount),
                        () -> userEnhancementCardRepository.save(
                                new UserEnhancementCard(userId, cardId, amount)));
        return cardId;
    }

    /**
     * 강화 카드 1장을 소모한다.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 ID
     * @return 마스터 ID
     */
    @Transactional
    public Long consumeOne(Long userId, Long cardId) {
        requireMasterExists(cardId);
        UserEnhancementCard owned = enhancementCardReader.getOwnedOrThrow(userId, cardId);
        try {
            owned.consumeOne();
        } catch (IllegalStateException ex) {
            throw new BadRequestException(ErrorCode.USER_ENHANCEMENT_CARD_INSUFFICIENT);
        }
        if (owned.getQuantity() == 0) {
            userEnhancementCardRepository.delete(owned);
        }
        return cardId;
    }

    private void requireMasterExists(Long cardId) {
        if (!enhancementCardRepository.existsById(cardId)) {
            throw new NotFoundException(ErrorCode.ENHANCEMENT_CARD_NOT_FOUND, "cardId=" + cardId);
        }
    }
}
