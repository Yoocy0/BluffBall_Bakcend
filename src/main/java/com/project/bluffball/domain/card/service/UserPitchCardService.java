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
 * 유저 구종 인스턴스 보유·강화·되돌리기 서비스.
 */
@Service
@RequiredArgsConstructor
public class UserPitchCardService {

    private final UserPitchCardValidator userPitchCardValidator;
    private final UserEnhancementCardValidator userEnhancementCardValidator;
    private final UserPitchCardReader userPitchCardReader;
    private final EnhancementCardReader enhancementCardReader;
    private final UserPitchCardExecutor userPitchCardExecutor;
    private final UserReader userReader;

    /**
     * 내 보유 구종 인스턴스 목록.
     *
     * @param userId 유저 ID
     * @return 인벤
     */
    public List<UserPitchCardResponse> getMyCards(Long userId) {
        return userPitchCardReader.getInventoryResponses(userId);
    }

    /**
     * 마스터 구종 기본본 인스턴스를 새로 획득한다.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return 새 인스턴스
     */
    public UserPitchCardResponse acquire(Long userId, Long cardId) {
        Long instanceId = userPitchCardExecutor.acquire(userId, cardId);
        return userPitchCardReader.getResponseByInstance(userId, instanceId);
    }

    /**
     * 미보유 마스터만 기본본 1장씩 지급한다.
     *
     * @param userId 유저 ID
     * @return 전체 인벤
     */
    public List<UserPitchCardResponse> acquireAllMissing(Long userId) {
        userPitchCardExecutor.acquireAllMissing(userId);
        return userPitchCardReader.getInventoryResponses(userId);
    }

    /**
     * 강화 카드로 인스턴스를 강화한다.
     *
     * @param userId          유저 ID
     * @param userPitchCardId 구종 인스턴스 ID
     * @param request         강화 카드 ID
     * @return 강화 후 인스턴스
     */
    public UserPitchCardResponse applyEnhancement(
            Long userId, Long userPitchCardId, ApplyEnhancementRequest request) {
        Long enhancementCardId = request.enhancementCardId();
        EnhancementEffect effect = enhancementCardReader.getEffect(enhancementCardId);

        userEnhancementCardValidator.validateSufficientQuantity(
                enhancementCardReader.getQuantity(userId, enhancementCardId), 1);

        switch (effect) {
            case CHANGE_AMOUNT_PLUS_1 -> userPitchCardValidator.validateChangeAmountNotEnhanced(
                    userPitchCardReader.isChangeAmountEnhanced(userId, userPitchCardId));
            case TIMING_FASTER -> {
                userPitchCardValidator.validateTimingNotEnhanced(
                        userPitchCardReader.getTimingEnhancement(userId, userPitchCardId));
                userPitchCardValidator.validateTimingBoundary(
                        userPitchCardReader.getBaseTimingForInstance(userId, userPitchCardId),
                        TimingEnhancement.FASTER);
            }
            case TIMING_SLOWER -> {
                userPitchCardValidator.validateTimingNotEnhanced(
                        userPitchCardReader.getTimingEnhancement(userId, userPitchCardId));
                userPitchCardValidator.validateTimingBoundary(
                        userPitchCardReader.getBaseTimingForInstance(userId, userPitchCardId),
                        TimingEnhancement.SLOWER);
            }
        }

        Long enhancedId = userPitchCardExecutor.applyEnhancement(
                userId, userPitchCardId, enhancementCardId, effect);
        return userPitchCardReader.getResponseByInstance(userId, enhancedId);
    }

    /**
     * 변화량 강화 되돌리기.
     *
     * @param userId          유저 ID
     * @param userPitchCardId 인스턴스 ID
     * @return 인스턴스
     */
    public UserPitchCardResponse revertChangeAmount(Long userId, Long userPitchCardId) {
        userPitchCardValidator.validateChangeAmountEnhanced(
                userPitchCardReader.isChangeAmountEnhanced(userId, userPitchCardId));
        userPitchCardValidator.validateCurrencyEnough(
                userReader.getCurrency(userId), EnhancementConstants.REVERT_COST);
        Long id = userPitchCardExecutor.revertChangeAmount(
                userId, userPitchCardId, EnhancementConstants.REVERT_COST);
        return userPitchCardReader.getResponseByInstance(userId, id);
    }

    /**
     * 타이밍 강화 되돌리기.
     *
     * @param userId          유저 ID
     * @param userPitchCardId 인스턴스 ID
     * @return 인스턴스
     */
    public UserPitchCardResponse revertTiming(Long userId, Long userPitchCardId) {
        userPitchCardValidator.validateTimingEnhanced(
                userPitchCardReader.getTimingEnhancement(userId, userPitchCardId));
        userPitchCardValidator.validateCurrencyEnough(
                userReader.getCurrency(userId), EnhancementConstants.REVERT_COST);
        Long id = userPitchCardExecutor.revertTiming(
                userId, userPitchCardId, EnhancementConstants.REVERT_COST);
        return userPitchCardReader.getResponseByInstance(userId, id);
    }
}
