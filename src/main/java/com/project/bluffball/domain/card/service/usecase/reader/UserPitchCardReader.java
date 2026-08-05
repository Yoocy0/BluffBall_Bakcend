package com.project.bluffball.domain.card.service.usecase.reader;

import com.project.bluffball.domain.card.dto.response.UserPitchCardResponse;
import com.project.bluffball.domain.card.entity.PitchCard;
import com.project.bluffball.domain.card.entity.UserPitchCard;
import com.project.bluffball.domain.card.enums.TimingEnhancement;
import com.project.bluffball.domain.card.repository.PitchCardRepository;
import com.project.bluffball.domain.card.repository.UserPitchCardRepository;
import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 유저 보유 구종 카드 읽기·실효 스탯 resolve Reader.
 */
@Component
@RequiredArgsConstructor
public class UserPitchCardReader {

    /** 유저 보유 구종 Repository */
    private final UserPitchCardRepository userPitchCardRepository;

    /** 마스터 구종 Repository */
    private final PitchCardRepository pitchCardRepository;

    /**
     * Entity 조회 (Executor·Reader 내부용).
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return 보유 카드
     */
    public UserPitchCard getByUserIdAndCardId(Long userId, Long cardId) {
        return userPitchCardRepository.findByUserIdAndCardId(userId, cardId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.USER_PITCH_CARD_NOT_FOUND,
                        "userId=" + userId + ", cardId=" + cardId));
    }

    /**
     * 보유 여부.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return 보유하면 true
     */
    public boolean exists(Long userId, Long cardId) {
        return userPitchCardRepository.existsByUserIdAndCardId(userId, cardId);
    }

    /**
     * 요청한 카드 ID를 모두 보유했는지.
     *
     * @param userId 유저 ID
     * @param cardIds 마스터 구종 ID 목록
     * @return 전부 보유하면 true
     */
    public boolean ownsAll(Long userId, Collection<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            return false;
        }
        Set<Long> unique = new HashSet<>(cardIds);
        List<UserPitchCard> owned = userPitchCardRepository.findByUserIdAndCardIdIn(userId, unique);
        return owned.size() == unique.size();
    }

    /**
     * 카드 코스트 합을 반환한다. 미보유 카드가 있으면 예외.
     *
     * @param userId 유저 ID
     * @param cardIds 마스터 구종 ID 목록
     * @return 코스트 합
     */
    public int sumCost(Long userId, List<Long> cardIds) {
        Map<Long, UserPitchCard> owned = userPitchCardRepository
                .findByUserIdAndCardIdIn(userId, cardIds)
                .stream()
                .collect(Collectors.toMap(UserPitchCard::getCardId, Function.identity()));
        int sum = 0;
        for (Long cardId : cardIds) {
            UserPitchCard card = owned.get(cardId);
            if (card == null) {
                throw new NotFoundException(
                        ErrorCode.USER_PITCH_CARD_NOT_FOUND,
                        "userId=" + userId + ", cardId=" + cardId);
            }
            sum += card.resolveCost();
        }
        return sum;
    }

    /**
     * 유저 보유 카드 DTO 목록을 반환한다. (Service ✅)
     *
     * @param userId 유저 ID
     * @return 보유 목록
     */
    public List<UserPitchCardResponse> getInventoryResponses(Long userId) {
        return userPitchCardRepository.findByUserIdOrderByCardIdAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 단일 보유 카드 DTO. (Service ✅)
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return 응답
     */
    public UserPitchCardResponse getResponse(Long userId, Long cardId) {
        return toResponse(getByUserIdAndCardId(userId, cardId));
    }

    /**
     * 변화량 강화 여부.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return 강화됨이면 true
     */
    public boolean isChangeAmountEnhanced(Long userId, Long cardId) {
        return getByUserIdAndCardId(userId, cardId).isChangeAmountEnhanced();
    }

    /**
     * 타이밍 강화 상태.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     * @return 강화 상태
     */
    public TimingEnhancement getTimingEnhancement(Long userId, Long cardId) {
        return getByUserIdAndCardId(userId, cardId).getTimingEnhancement();
    }

    /**
     * 실효 타이밍 (미보유면 마스터 타이밍).
     *
     * @param userId 유저 ID (nullable이면 마스터만)
     * @param cardId 마스터 구종 ID
     * @return 실효 타이밍
     */
    public Timing getEffectiveTiming(Long userId, Long cardId) {
        PitchCard master = requireMaster(cardId);
        if (userId == null) {
            return master.getTiming();
        }
        return findOwned(userId, cardId)
                .map(owned -> owned.resolveEffectiveTiming(master.getTiming()))
                .orElse(master.getTiming());
    }

    /**
     * 실효 변화량 (미보유면 마스터 변화량).
     *
     * @param userId 유저 ID (nullable이면 마스터만)
     * @param cardId 마스터 구종 ID
     * @return 실효 변화량
     */
    public int getEffectiveChangeAmount(Long userId, Long cardId) {
        PitchCard master = requireMaster(cardId);
        if (userId == null) {
            return master.getChangeAmount();
        }
        return findOwned(userId, cardId)
                .map(owned -> owned.resolveEffectiveChangeAmount(master.getChangeAmount()))
                .orElse(master.getChangeAmount());
    }

    /**
     * 실효 스탯으로 최종 좌표를 계산한다.
     *
     * @param userId 투수 유저 ID
     * @param cardId 마스터 구종 ID
     * @param startCoordinateNumber 시작 좌표
     * @return 최종 좌표
     */
    public int calculateFinalCoordinateNumber(Long userId, Long cardId, int startCoordinateNumber) {
        PitchCard master = requireMaster(cardId);
        int effectiveAmount = getEffectiveChangeAmount(userId, cardId);
        return master.calculateFinalCoordinateNumber(startCoordinateNumber, effectiveAmount);
    }

    /**
     * 핸드 카드의 실효 CardInfo 목록. (Service ✅)
     *
     * @param userId 투수 유저 ID
     * @param cardIds 마스터 구종 ID 목록
     * @return CardInfo 목록
     */
    public List<CardInfo> getEffectiveCardInfos(Long userId, List<Long> cardIds) {
        return cardIds.stream()
                .map(cardId -> {
                    PitchCard master = requireMaster(cardId);
                    return new CardInfo(
                            master.getId(),
                            master.getName(),
                            getEffectiveChangeAmount(userId, cardId),
                            master.getDirection(),
                            getEffectiveTiming(userId, cardId));
                })
                .toList();
    }

    /**
     * 마스터 구종 타이밍을 반환한다.
     *
     * @param cardId 마스터 구종 ID
     * @return 마스터 타이밍
     */
    public Timing getBaseTiming(Long cardId) {
        return requireMaster(cardId).getTiming();
    }

    private Optional<UserPitchCard> findOwned(Long userId, Long cardId) {
        return userPitchCardRepository.findByUserIdAndCardId(userId, cardId);
    }

    private PitchCard requireMaster(Long cardId) {
        return pitchCardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PITCH_CARD_NOT_FOUND, "cardId=" + cardId));
    }

    private UserPitchCardResponse toResponse(UserPitchCard owned) {
        PitchCard master = requireMaster(owned.getCardId());
        return new UserPitchCardResponse(
                master.getId(),
                master.getName(),
                master.getDirection(),
                master.getChangeAmount(),
                owned.resolveEffectiveChangeAmount(master.getChangeAmount()),
                owned.isChangeAmountEnhanced(),
                master.getTiming(),
                owned.resolveEffectiveTiming(master.getTiming()),
                owned.getTimingEnhancement(),
                owned.resolveCost());
    }
}
