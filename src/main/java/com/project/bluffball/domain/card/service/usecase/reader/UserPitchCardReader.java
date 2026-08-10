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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 유저 보유 구종 인스턴스 읽기·실효 스탯 resolve Reader.
 *
 * <p>인게임 핸드 ID는 주로 {@code UserPitchCard} 인스턴스 ID이다.
 * resolve 시 인스턴스 소유를 먼저 보고, 없으면 마스터 기준으로 보유 인스턴스 중
 * 강화가 가장 많은 장을 사용한다.</p>
 */
@Component
@RequiredArgsConstructor
public class UserPitchCardReader {

    /** 유저 보유 구종 Repository */
    private final UserPitchCardRepository userPitchCardRepository;

    /** 마스터 구종 Repository */
    private final PitchCardRepository pitchCardRepository;

    /**
     * 유저가 보유한 모든 구종 인스턴스 ID 목록.
     *
     * <p>SHOWDOWN/CUSTOM/BOT 사람 측 드로우·멀리건 풀로 사용한다.</p>
     *
     * @param userId 유저 ID
     * @return 보유 인스턴스 ID (마스터 ID·인스턴스 ID 순)
     */
    public List<Long> findOwnedInstanceIds(Long userId) {
        return userPitchCardRepository.findByUserIdOrderByCardIdAscIdAsc(userId).stream()
                .map(UserPitchCard::getId)
                .toList();
    }

    /**
     * 인스턴스 Entity 조회 (Executor·Reader 내부용).
     *
     * @param userId           유저 ID
     * @param userPitchCardId  인스턴스 ID
     * @return 보유 인스턴스
     */
    public UserPitchCard getByIdForUser(Long userId, Long userPitchCardId) {
        return userPitchCardRepository.findByIdAndUserId(userPitchCardId, userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.USER_PITCH_CARD_NOT_FOUND,
                        "userId=" + userId + ", userPitchCardId=" + userPitchCardId));
    }

    /**
     * 마스터 구종 인스턴스 존재 여부.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 ID
     * @return 1장 이상이면 true
     */
    public boolean existsMaster(Long userId, Long cardId) {
        return userPitchCardRepository.existsByUserIdAndCardId(userId, cardId);
    }

    /**
     * 미강화 기본본 인스턴스가 있는지.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 ID
     * @return 기본본이 있으면 true
     */
    public boolean hasUnenhancedInstance(Long userId, Long cardId) {
        return userPitchCardRepository.findByUserIdAndCardId(userId, cardId).stream()
                .anyMatch(UserPitchCard::isBaseCopy);
    }

    /**
     * 로드아웃 인스턴스 ID를 모두 보유했는지 (중복 ID 불가).
     *
     * @param userId            유저 ID
     * @param userPitchCardIds  인스턴스 ID 목록
     * @return 전부 보유·중복 없으면 true
     */
    public boolean ownsAllInstances(Long userId, Collection<Long> userPitchCardIds) {
        if (userPitchCardIds == null || userPitchCardIds.isEmpty()) {
            return false;
        }
        Set<Long> unique = new HashSet<>(userPitchCardIds);
        if (unique.size() != userPitchCardIds.size()) {
            return false;
        }
        List<UserPitchCard> owned = userPitchCardRepository.findByUserIdAndIdIn(userId, unique);
        return owned.size() == unique.size();
    }

    /**
     * 로드아웃용 인스턴스가 유효한 구종인지 판정한다.
     *
     * <p>중복 없고, 유저 소유이며, 각 인스턴스의 마스터가 존재하는 구종이어야 한다.</p>
     *
     * @param userId           유저 ID
     * @param userPitchCardIds 인스턴스 ID 목록
     * @return 유효하면 true
     */
    public boolean areValidPitchInstances(Long userId, List<Long> userPitchCardIds) {
        if (!ownsAllInstances(userId, userPitchCardIds)) {
            return false;
        }
        List<UserPitchCard> owned = userPitchCardRepository.findByUserIdAndIdIn(userId, userPitchCardIds);
        return owned.stream().allMatch(instance -> pitchCardRepository.existsById(instance.getCardId()));
    }

    /**
     * 인스턴스 코스트 합.
     *
     * @param userId           유저 ID
     * @param userPitchCardIds 인스턴스 ID 목록
     * @return 코스트 합
     */
    public int sumInstanceCost(Long userId, List<Long> userPitchCardIds) {
        Map<Long, UserPitchCard> owned = userPitchCardRepository
                .findByUserIdAndIdIn(userId, userPitchCardIds)
                .stream()
                .collect(Collectors.toMap(UserPitchCard::getId, Function.identity()));
        int sum = 0;
        for (Long id : userPitchCardIds) {
            UserPitchCard card = owned.get(id);
            if (card == null) {
                throw new NotFoundException(
                        ErrorCode.USER_PITCH_CARD_NOT_FOUND,
                        "userId=" + userId + ", userPitchCardId=" + id);
            }
            sum += card.resolveCost();
        }
        return sum;
    }

    /**
     * 유저 인벤 응답.
     *
     * @param userId 유저 ID
     * @return 인스턴스 목록
     */
    public List<UserPitchCardResponse> getInventoryResponses(Long userId) {
        return userPitchCardRepository.findByUserIdOrderByCardIdAscIdAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 인스턴스 응답.
     *
     * @param userId          유저 ID
     * @param userPitchCardId 인스턴스 ID
     * @return 응답
     */
    public UserPitchCardResponse getResponseByInstance(Long userId, Long userPitchCardId) {
        return toResponse(getByIdForUser(userId, userPitchCardId));
    }

    /**
     * 변화량 강화 여부 (인스턴스).
     *
     * @param userId          유저 ID
     * @param userPitchCardId 인스턴스 ID
     * @return 강화됨이면 true
     */
    public boolean isChangeAmountEnhanced(Long userId, Long userPitchCardId) {
        return getByIdForUser(userId, userPitchCardId).isChangeAmountEnhanced();
    }

    /**
     * 타이밍 강화 상태 (인스턴스).
     *
     * @param userId          유저 ID
     * @param userPitchCardId 인스턴스 ID
     * @return 강화 상태
     */
    public TimingEnhancement getTimingEnhancement(Long userId, Long userPitchCardId) {
        return getByIdForUser(userId, userPitchCardId).getTimingEnhancement();
    }

    /**
     * 인스턴스의 마스터 구종 ID.
     *
     * @param userId          유저 ID
     * @param userPitchCardId 인스턴스 ID
     * @return 마스터 ID
     */
    public Long getMasterCardId(Long userId, Long userPitchCardId) {
        return getByIdForUser(userId, userPitchCardId).getCardId();
    }

    /**
     * 핸드 ID(인스턴스 또는 마스터)를 마스터 구종 ID로 변환한다.
     *
     * <p>해당 유저 인스턴스면 마스터 ID, 아니면 인자 그대로(마스터로 간주).</p>
     *
     * @param userId           투수 유저 ID (null이면 마스터로 간주)
     * @param cardOrInstanceId 인스턴스 또는 마스터 ID
     * @return 마스터 구종 ID
     */
    public Long resolveMasterCardId(Long userId, Long cardOrInstanceId) {
        return resolveMasterId(userId, cardOrInstanceId);
    }

    /**
     * 실효 타이밍.
     *
     * <p>{@code cardOrInstanceId}가 유저 인스턴스면 그 오버레이,
     * 아니면 마스터 ID로 보고 보유 인스턴스 중 강화가 가장 많은 장을 사용한다.</p>
     *
     * @param userId            유저 ID (null이면 마스터만)
     * @param cardOrInstanceId  인스턴스 또는 마스터 ID
     * @return 실효 타이밍
     */
    public Timing getEffectiveTiming(Long userId, Long cardOrInstanceId) {
        if (userId != null) {
            Optional<UserPitchCard> instance = userPitchCardRepository
                    .findByIdAndUserId(cardOrInstanceId, userId);
            if (instance.isPresent()) {
                PitchCard master = requireMaster(instance.get().getCardId());
                return instance.get().resolveEffectiveTiming(master.getTiming());
            }
        }
        PitchCard master = requireMaster(cardOrInstanceId);
        if (userId == null) {
            return master.getTiming();
        }
        return pickBestOwned(userId, cardOrInstanceId)
                .map(owned -> owned.resolveEffectiveTiming(master.getTiming()))
                .orElse(master.getTiming());
    }

    /**
     * 실효 변화량.
     *
     * @param userId           유저 ID
     * @param cardOrInstanceId 인스턴스 또는 마스터 ID
     * @return 실효 변화량
     */
    public int getEffectiveChangeAmount(Long userId, Long cardOrInstanceId) {
        if (userId != null) {
            Optional<UserPitchCard> instance = userPitchCardRepository
                    .findByIdAndUserId(cardOrInstanceId, userId);
            if (instance.isPresent()) {
                PitchCard master = requireMaster(instance.get().getCardId());
                return instance.get().resolveEffectiveChangeAmount(master.getChangeAmount());
            }
        }
        PitchCard master = requireMaster(cardOrInstanceId);
        if (userId == null) {
            return master.getChangeAmount();
        }
        return pickBestOwned(userId, cardOrInstanceId)
                .map(owned -> owned.resolveEffectiveChangeAmount(master.getChangeAmount()))
                .orElse(master.getChangeAmount());
    }

    /**
     * 실효 스탯으로 최종 좌표를 계산한다.
     *
     * @param userId                투수 유저 ID
     * @param cardOrInstanceId      인스턴스 또는 마스터 ID
     * @param startCoordinateNumber 시작 좌표
     * @return 최종 좌표
     */
    public int calculateFinalCoordinateNumber(
            Long userId, Long cardOrInstanceId, int startCoordinateNumber) {
        Long masterId = resolveMasterId(userId, cardOrInstanceId);
        PitchCard master = requireMaster(masterId);
        int effectiveAmount = getEffectiveChangeAmount(userId, cardOrInstanceId);
        return master.calculateFinalCoordinateNumber(startCoordinateNumber, effectiveAmount);
    }

    /**
     * 핸드 카드의 실효 CardInfo 목록.
     *
     * <p>핸드에 담긴 ID를 그대로 {@code CardInfo.cardId}에 넣어 선택 키로 쓴다.</p>
     *
     * @param userId 투수 유저 ID
     * @param handIds 핸드 ID (인스턴스 또는 마스터)
     * @return CardInfo 목록
     */
    public List<CardInfo> getEffectiveCardInfos(Long userId, List<Long> handIds) {
        return handIds.stream()
                .map(handId -> {
                    Long masterId = resolveMasterId(userId, handId);
                    PitchCard master = requireMaster(masterId);
                    return new CardInfo(
                            handId,
                            master.getName(),
                            getEffectiveChangeAmount(userId, handId),
                            master.getDirection(),
                            getEffectiveTiming(userId, handId));
                })
                .toList();
    }

    /**
     * 마스터 구종 타이밍.
     *
     * @param cardId 마스터 ID
     * @return 타이밍
     */
    public Timing getBaseTiming(Long cardId) {
        return requireMaster(cardId).getTiming();
    }

    /**
     * 인스턴스의 마스터 타이밍 (강화 경계 검증용).
     *
     * @param userId          유저 ID
     * @param userPitchCardId 인스턴스 ID
     * @return 마스터 타이밍
     */
    public Timing getBaseTimingForInstance(Long userId, Long userPitchCardId) {
        return requireMaster(getMasterCardId(userId, userPitchCardId)).getTiming();
    }

    private Long resolveMasterId(Long userId, Long cardOrInstanceId) {
        if (userId != null) {
            Optional<UserPitchCard> instance = userPitchCardRepository
                    .findByIdAndUserId(cardOrInstanceId, userId);
            if (instance.isPresent()) {
                return instance.get().getCardId();
            }
        }
        return cardOrInstanceId;
    }

    private Optional<UserPitchCard> pickBestOwned(Long userId, Long masterCardId) {
        return userPitchCardRepository.findByUserIdAndCardId(userId, masterCardId).stream()
                .max(Comparator.comparingInt(UserPitchCard::enhancementCount)
                        .thenComparing(UserPitchCard::getId));
    }

    private PitchCard requireMaster(Long cardId) {
        return pitchCardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PITCH_CARD_NOT_FOUND, "cardId=" + cardId));
    }

    private UserPitchCardResponse toResponse(UserPitchCard owned) {
        PitchCard master = requireMaster(owned.getCardId());
        return new UserPitchCardResponse(
                owned.getId(),
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
