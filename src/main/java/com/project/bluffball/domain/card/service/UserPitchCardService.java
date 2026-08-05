package com.project.bluffball.domain.card.service;

import com.project.bluffball.domain.card.dto.request.EnhanceTimingRequest;
import com.project.bluffball.domain.card.dto.response.UserPitchCardResponse;
import com.project.bluffball.domain.card.enums.TimingEnhancement;
import com.project.bluffball.domain.card.service.usecase.executor.UserPitchCardExecutor;
import com.project.bluffball.domain.card.service.usecase.reader.UserPitchCardReader;
import com.project.bluffball.domain.card.service.usecase.validator.UserPitchCardValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 유저 구종 카드 보유·강화 서비스.
 */
@Service
@RequiredArgsConstructor
public class UserPitchCardService {

    /** 강화·보유 검증 */
    private final UserPitchCardValidator userPitchCardValidator;

    /** 보유 카드 Reader */
    private final UserPitchCardReader userPitchCardReader;

    /** 획득·강화 Executor */
    private final UserPitchCardExecutor userPitchCardExecutor;

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
     * 변화량 강화를 적용한다.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return 강화 후 카드
     */
    public UserPitchCardResponse enhanceChangeAmount(Long userId, Long cardId) {
        userPitchCardValidator.validateChangeAmountNotEnhanced(
                userPitchCardReader.isChangeAmountEnhanced(userId, cardId));
        Long enhancedId = userPitchCardExecutor.enhanceChangeAmount(userId, cardId);
        return userPitchCardReader.getResponse(userId, enhancedId);
    }

    /**
     * 타이밍 강화를 적용한다.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @param request 강화 방향
     * @return 강화 후 카드
     */
    public UserPitchCardResponse enhanceTiming(Long userId, Long cardId, EnhanceTimingRequest request) {
        TimingEnhancement enhancement = request.timingEnhancement();
        userPitchCardValidator.validateTimingEnhancementDirection(enhancement);
        userPitchCardValidator.validateTimingNotEnhanced(
                userPitchCardReader.getTimingEnhancement(userId, cardId));
        userPitchCardValidator.validateTimingBoundary(
                userPitchCardReader.getBaseTiming(cardId), enhancement);

        Long enhancedId = userPitchCardExecutor.enhanceTiming(userId, cardId, enhancement);
        return userPitchCardReader.getResponse(userId, enhancedId);
    }
}
