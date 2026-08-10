package com.project.bluffball.domain.card.service;

import com.project.bluffball.domain.card.EnhancementConstants;
import com.project.bluffball.domain.card.dto.request.ApplyEnhancementRequest;
import com.project.bluffball.domain.card.dto.response.UserPitchCardResponse;
import com.project.bluffball.domain.card.enums.EnhancementEffect;
import com.project.bluffball.domain.card.enums.TimingEnhancement;
import com.project.bluffball.domain.card.service.usecase.executor.UserPitchCardExecutor;
import com.project.bluffball.domain.card.service.usecase.reader.EnhancementCardReader;
import com.project.bluffball.domain.card.service.usecase.reader.UserPitchCardReader;
import com.project.bluffball.domain.card.service.usecase.validator.UserEnhancementCardValidator;
import com.project.bluffball.domain.card.service.usecase.validator.UserPitchCardValidator;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 유저 구종 카드 보유·강화·되돌리기 서비스.
 */
@Service
@RequiredArgsConstructor
public class UserPitchCardService {

    /** 강화·보유 검증 */
    private final UserPitchCardValidator userPitchCardValidator;

    /** 강화 카드 수량 검증 */
    private final UserEnhancementCardValidator userEnhancementCardValidator;

    /** 보유 카드 Reader */
    private final UserPitchCardReader userPitchCardReader;

    /** 강화 카드 Reader */
    private final EnhancementCardReader enhancementCardReader;

    /** 획득·강화 Executor */
    private final UserPitchCardExecutor userPitchCardExecutor;

    /** 유저 Reader */
    private final UserReader userReader;

    /**
     * 내 보유 구종 카드 목록을 조회한다.
     *
     * @param userId 유저 ID
     * @return 보유 목록
     */
    public List<UserPitchCardResponse> getMyCards(Long userId) {
        return userPitchCardReader.getInventoryResponses(userId);
    }

    /**
     * 마스터 구종 1장을 획득한다.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return 획득한 카드
     */
    public UserPitchCardResponse acquire(Long userId, Long cardId) {
        userPitchCardValidator.validateNotOwned(userPitchCardReader.exists(userId, cardId));
        Long acquiredId = userPitchCardExecutor.acquire(userId, cardId);
        return userPitchCardReader.getResponse(userId, acquiredId);
    }

    /**
     * 미보유 마스터 구종을 모두 획득한다.
     *
     * @param userId 유저 ID
     * @return 갱신된 전체 보유 목록
     */
    public List<UserPitchCardResponse> acquireAllMissing(Long userId) {
        userPitchCardExecutor.acquireAllMissing(userId);
        return userPitchCardReader.getInventoryResponses(userId);
    }

    /**
     * 강화 카드를 소모하여 구종에 강화를 적용한다.
     *
     * @param userId  유저 ID
     * @param cardId  마스터 구종 ID
     * @param request 강화 카드 ID
     * @return 강화 후 구종
     */
    public UserPitchCardResponse applyEnhancement(
            Long userId, Long cardId, ApplyEnhancementRequest request) {
        Long enhancementCardId = request.enhancementCardId();
        EnhancementEffect effect = enhancementCardReader.getEffect(enhancementCardId);

        userEnhancementCardValidator.validateSufficientQuantity(
                enhancementCardReader.getQuantity(userId, enhancementCardId), 1);

        switch (effect) {
            case CHANGE_AMOUNT_PLUS_1 -> userPitchCardValidator.validateChangeAmountNotEnhanced(
                    userPitchCardReader.isChangeAmountEnhanced(userId, cardId));
            case TIMING_FASTER -> {
                userPitchCardValidator.validateTimingNotEnhanced(
                        userPitchCardReader.getTimingEnhancement(userId, cardId));
                userPitchCardValidator.validateTimingBoundary(
                        userPitchCardReader.getBaseTiming(cardId), TimingEnhancement.FASTER);
            }
            case TIMING_SLOWER -> {
                userPitchCardValidator.validateTimingNotEnhanced(
                        userPitchCardReader.getTimingEnhancement(userId, cardId));
                userPitchCardValidator.validateTimingBoundary(
                        userPitchCardReader.getBaseTiming(cardId), TimingEnhancement.SLOWER);
            }
        }

        Long enhancedId = userPitchCardExecutor.applyEnhancement(
                userId, cardId, enhancementCardId, effect);
        return userPitchCardReader.getResponse(userId, enhancedId);
    }

    /**
     * 변화량 강화를 재화로 되돌린다.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return 되돌린 구종
     */
    public UserPitchCardResponse revertChangeAmount(Long userId, Long cardId) {
        userPitchCardValidator.validateChangeAmountEnhanced(
                userPitchCardReader.isChangeAmountEnhanced(userId, cardId));
        userPitchCardValidator.validateCurrencyEnough(
                userReader.getCurrency(userId), EnhancementConstants.REVERT_COST);
        Long revertedId = userPitchCardExecutor.revertChangeAmount(
                userId, cardId, EnhancementConstants.REVERT_COST);
        return userPitchCardReader.getResponse(userId, revertedId);
    }

    /**
     * 타이밍 강화를 재화로 되돌린다.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return 되돌린 구종
     */
    public UserPitchCardResponse revertTiming(Long userId, Long cardId) {
        userPitchCardValidator.validateTimingEnhanced(
                userPitchCardReader.getTimingEnhancement(userId, cardId));
        userPitchCardValidator.validateCurrencyEnough(
                userReader.getCurrency(userId), EnhancementConstants.REVERT_COST);
        Long revertedId = userPitchCardExecutor.revertTiming(
                userId, cardId, EnhancementConstants.REVERT_COST);
        return userPitchCardReader.getResponse(userId, revertedId);
    }
}
