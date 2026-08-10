package com.project.bluffball.domain.card.service.usecase.executor;

import com.project.bluffball.domain.card.entity.UserPitchCard;
import com.project.bluffball.domain.card.enums.EnhancementEffect;
import com.project.bluffball.domain.card.enums.TimingEnhancement;
import com.project.bluffball.domain.card.repository.PitchCardRepository;
import com.project.bluffball.domain.card.repository.UserPitchCardRepository;
import com.project.bluffball.domain.card.service.usecase.reader.UserPitchCardReader;
import com.project.bluffball.domain.user.entity.User;
import com.project.bluffball.domain.user.repository.UserRepository;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 유저 구종 카드 획득·강화·되돌리기 쓰기 Executor.
 */
@Component
@RequiredArgsConstructor
public class UserPitchCardExecutor {

    /** 유저 보유 Repository */
    private final UserPitchCardRepository userPitchCardRepository;

    /** 마스터 구종 Repository */
    private final PitchCardRepository pitchCardRepository;

    /** 유저 구종 Reader */
    private final UserPitchCardReader userPitchCardReader;

    /** 강화 카드 소모 Executor */
    private final UserEnhancementCardExecutor userEnhancementCardExecutor;

    /** 유저 Reader */
    private final UserReader userReader;

    /** 유저 Repository */
    private final UserRepository userRepository;

    /**
     * 구종 카드를 획득한다 (미강화).
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return 마스터 카드 ID
     */
    @Transactional
    public Long acquire(Long userId, Long cardId) {
        requireMasterExists(cardId);
        userPitchCardRepository.save(new UserPitchCard(userId, cardId));
        return cardId;
    }

    /**
     * 미보유 마스터 구종을 모두 획득한다.
     *
     * @param userId 유저 ID
     * @return 새로 획득한 마스터 카드 ID 목록
     */
    @Transactional
    public List<Long> acquireAllMissing(Long userId) {
        List<Long> acquired = new ArrayList<>();
        for (Long cardId : pitchCardRepository.findAllIds()) {
            if (!userPitchCardRepository.existsByUserIdAndCardId(userId, cardId)) {
                userPitchCardRepository.save(new UserPitchCard(userId, cardId));
                acquired.add(cardId);
            }
        }
        return acquired;
    }

    /**
     * 미보유 카드만 획득한다.
     *
     * @param userId  유저 ID
     * @param cardIds 마스터 구종 ID 목록
     * @return 새로 획득한 마스터 카드 ID 목록
     */
    @Transactional
    public List<Long> acquireIfMissing(Long userId, List<Long> cardIds) {
        List<Long> acquired = new ArrayList<>();
        for (Long cardId : cardIds) {
            requireMasterExists(cardId);
            if (!userPitchCardRepository.existsByUserIdAndCardId(userId, cardId)) {
                userPitchCardRepository.save(new UserPitchCard(userId, cardId));
                acquired.add(cardId);
            }
        }
        return acquired;
    }

    /**
     * 강화 카드를 소모하고 구종에 효과를 적용한다.
     *
     * @param userId             유저 ID
     * @param pitchCardId        마스터 구종 ID
     * @param enhancementCardId  강화 카드 마스터 ID
     * @param effect             강화 효과
     * @return 구종 마스터 ID
     */
    @Transactional
    public Long applyEnhancement(
            Long userId,
            Long pitchCardId,
            Long enhancementCardId,
            EnhancementEffect effect) {
        userEnhancementCardExecutor.consumeOne(userId, enhancementCardId);
        UserPitchCard owned = userPitchCardReader.getByUserIdAndCardId(userId, pitchCardId);
        try {
            switch (effect) {
                case CHANGE_AMOUNT_PLUS_1 -> owned.enhanceChangeAmount();
                case TIMING_FASTER -> owned.enhanceTiming(TimingEnhancement.FASTER);
                case TIMING_SLOWER -> owned.enhanceTiming(TimingEnhancement.SLOWER);
            }
        } catch (IllegalStateException ex) {
            throw new ConflictException(ErrorCode.USER_PITCH_CARD_ALREADY_ENHANCED, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ErrorCode.USER_PITCH_CARD_TIMING_INVALID, ex.getMessage());
        }
        return pitchCardId;
    }

    /**
     * 변화량 강화를 재화로 되돌린다 (강화 카드 미환불).
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @param cost   재화 비용
     * @return 구종 마스터 ID
     */
    @Transactional
    public Long revertChangeAmount(Long userId, Long cardId, long cost) {
        spendCurrency(userId, cost);
        UserPitchCard owned = userPitchCardReader.getByUserIdAndCardId(userId, cardId);
        try {
            owned.revertChangeAmount();
        } catch (IllegalStateException ex) {
            throw new BadRequestException(ErrorCode.USER_PITCH_CARD_NOT_ENHANCED, ex.getMessage());
        }
        return cardId;
    }

    /**
     * 타이밍 강화를 재화로 되돌린다 (강화 카드 미환불).
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @param cost   재화 비용
     * @return 구종 마스터 ID
     */
    @Transactional
    public Long revertTiming(Long userId, Long cardId, long cost) {
        spendCurrency(userId, cost);
        UserPitchCard owned = userPitchCardReader.getByUserIdAndCardId(userId, cardId);
        try {
            owned.revertTiming();
        } catch (IllegalStateException ex) {
            throw new BadRequestException(ErrorCode.USER_PITCH_CARD_NOT_ENHANCED, ex.getMessage());
        }
        return cardId;
    }

    private void spendCurrency(Long userId, long cost) {
        User user = userReader.getById(userId);
        try {
            user.spendCurrency(cost);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ErrorCode.USER_CURRENCY_INSUFFICIENT, ex.getMessage());
        }
        userRepository.save(user);
    }

    private void requireMasterExists(Long cardId) {
        if (!pitchCardRepository.existsById(cardId)) {
            throw new NotFoundException(ErrorCode.PITCH_CARD_NOT_FOUND, "cardId=" + cardId);
        }
    }
}
