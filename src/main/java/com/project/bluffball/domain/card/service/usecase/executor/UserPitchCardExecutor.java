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
 * 유저 구종 인스턴스 획득·강화·되돌리기 Executor.
 */
@Component
@RequiredArgsConstructor
public class UserPitchCardExecutor {

    private final UserPitchCardRepository userPitchCardRepository;
    private final PitchCardRepository pitchCardRepository;
    private final UserPitchCardReader userPitchCardReader;
    private final UserEnhancementCardExecutor userEnhancementCardExecutor;
    private final UserReader userReader;
    private final UserRepository userRepository;

    /**
     * 기본본 인스턴스를 새로 생성한다.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return 생성된 인스턴스 ID
     */
    @Transactional
    public Long acquire(Long userId, Long cardId) {
        requireMasterExists(cardId);
        UserPitchCard saved = userPitchCardRepository.save(new UserPitchCard(userId, cardId));
        return saved.getId();
    }

    /**
     * 마스터별로 인스턴스가 하나도 없으면 기본본 1장을 지급한다.
     *
     * @param userId 유저 ID
     * @return 새로 만든 인스턴스 ID 목록
     */
    @Transactional
    public List<Long> acquireAllMissing(Long userId) {
        List<Long> acquired = new ArrayList<>();
        for (Long cardId : pitchCardRepository.findAllIds()) {
            if (!userPitchCardRepository.existsByUserIdAndCardId(userId, cardId)) {
                acquired.add(acquire(userId, cardId));
            }
        }
        return acquired;
    }

    /**
     * 지정 마스터에 대해 인스턴스가 없으면 기본본을 지급하고, 각 마스터의 인스턴스 ID를 반환한다.
     *
     * <p>이미 보유 중이면 기본본을 우선, 없으면 첫 인스턴스 ID를 반환한다.</p>
     *
     * @param userId  유저 ID
     * @param cardIds 마스터 ID 목록
     * @return 마스터별 인스턴스 ID (입력 순서)
     */
    @Transactional
    public List<Long> acquireIfMissing(Long userId, List<Long> cardIds) {
        List<Long> instanceIds = new ArrayList<>();
        for (Long cardId : cardIds) {
            requireMasterExists(cardId);
            List<UserPitchCard> existing = userPitchCardRepository.findByUserIdAndCardId(userId, cardId);
            if (existing.isEmpty()) {
                instanceIds.add(acquire(userId, cardId));
            } else {
                Long id = existing.stream()
                        .filter(UserPitchCard::isBaseCopy)
                        .map(UserPitchCard::getId)
                        .findFirst()
                        .orElseGet(() -> existing.get(0).getId());
                instanceIds.add(id);
            }
        }
        return instanceIds;
    }

    /**
     * 강화 카드를 소모하고 인스턴스에 효과를 적용한다.
     *
     * @param userId            유저 ID
     * @param userPitchCardId   구종 인스턴스 ID
     * @param enhancementCardId 강화 카드 마스터 ID
     * @param effect            효과
     * @return 인스턴스 ID
     */
    @Transactional
    public Long applyEnhancement(
            Long userId,
            Long userPitchCardId,
            Long enhancementCardId,
            EnhancementEffect effect) {
        userEnhancementCardExecutor.consumeOne(userId, enhancementCardId);
        UserPitchCard owned = userPitchCardReader.getByIdForUser(userId, userPitchCardId);
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
        return userPitchCardId;
    }

    /**
     * 변화량 강화를 재화로 되돌린다.
     *
     * @param userId          유저 ID
     * @param userPitchCardId 인스턴스 ID
     * @param cost            재화
     * @return 인스턴스 ID
     */
    @Transactional
    public Long revertChangeAmount(Long userId, Long userPitchCardId, long cost) {
        spendCurrency(userId, cost);
        UserPitchCard owned = userPitchCardReader.getByIdForUser(userId, userPitchCardId);
        try {
            owned.revertChangeAmount();
        } catch (IllegalStateException ex) {
            throw new BadRequestException(ErrorCode.USER_PITCH_CARD_NOT_ENHANCED, ex.getMessage());
        }
        return userPitchCardId;
    }

    /**
     * 타이밍 강화를 재화로 되돌린다.
     *
     * @param userId          유저 ID
     * @param userPitchCardId 인스턴스 ID
     * @param cost            재화
     * @return 인스턴스 ID
     */
    @Transactional
    public Long revertTiming(Long userId, Long userPitchCardId, long cost) {
        spendCurrency(userId, cost);
        UserPitchCard owned = userPitchCardReader.getByIdForUser(userId, userPitchCardId);
        try {
            owned.revertTiming();
        } catch (IllegalStateException ex) {
            throw new BadRequestException(ErrorCode.USER_PITCH_CARD_NOT_ENHANCED, ex.getMessage());
        }
        return userPitchCardId;
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
