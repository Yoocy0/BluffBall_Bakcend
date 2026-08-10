package com.project.bluffball.domain.card.service.usecase.reader;

import com.project.bluffball.domain.card.dto.response.EnhancementCardResponse;
import com.project.bluffball.domain.card.dto.response.UserEnhancementCardResponse;
import com.project.bluffball.domain.card.entity.EnhancementCard;
import com.project.bluffball.domain.card.entity.UserEnhancementCard;
import com.project.bluffball.domain.card.enums.EnhancementEffect;
import com.project.bluffball.domain.card.repository.EnhancementCardRepository;
import com.project.bluffball.domain.card.repository.UserEnhancementCardRepository;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 강화 카드 마스터·유저 보유 읽기 Reader.
 */
@Component
@RequiredArgsConstructor
public class EnhancementCardReader {

    /** 마스터 Repository */
    private final EnhancementCardRepository enhancementCardRepository;

    /** 유저 보유 Repository */
    private final UserEnhancementCardRepository userEnhancementCardRepository;

    /**
     * Entity 조회 (Executor·Reader 내부용).
     *
     * @param cardId 마스터 ID
     * @return 강화 카드
     */
    public EnhancementCard getById(Long cardId) {
        return enhancementCardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.ENHANCEMENT_CARD_NOT_FOUND, "cardId=" + cardId));
    }

    /**
     * 효과로 마스터를 조회한다.
     *
     * @param effect 효과
     * @return 강화 카드
     */
    public EnhancementCard getByEffect(EnhancementEffect effect) {
        return enhancementCardRepository.findByEffect(effect)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.ENHANCEMENT_CARD_NOT_FOUND, "effect=" + effect));
    }

    /**
     * 마스터 효과 enum을 반환한다 (Service ✅).
     *
     * @param cardId 마스터 ID
     * @return 효과
     */
    public EnhancementEffect getEffect(Long cardId) {
        return getById(cardId).getEffect();
    }

    /**
     * 마스터 존재 여부.
     *
     * @param cardId 마스터 ID
     * @return 존재하면 true
     */
    public boolean exists(Long cardId) {
        return enhancementCardRepository.existsById(cardId);
    }

    /**
     * 마스터 전체 목록 응답.
     *
     * @return 카탈로그
     */
    public List<EnhancementCardResponse> getCatalogResponses() {
        return enhancementCardRepository.findAll().stream()
                .sorted(Comparator.comparing(c -> c.getEffect().ordinal()))
                .map(EnhancementCardResponse::from)
                .toList();
    }

    /**
     * 유저 보유 수량을 반환한다 (없으면 0).
     *
     * @param userId 유저 ID
     * @param cardId 마스터 ID
     * @return 수량
     */
    public int getQuantity(Long userId, Long cardId) {
        return userEnhancementCardRepository.findByUserIdAndEnhancementCardId(userId, cardId)
                .map(UserEnhancementCard::getQuantity)
                .orElse(0);
    }

    /**
     * 유저 보유 행 조회 (Executor 내부용).
     *
     * @param userId 유저 ID
     * @param cardId 마스터 ID
     * @return 보유 행
     */
    public UserEnhancementCard getOwnedOrThrow(Long userId, Long cardId) {
        return userEnhancementCardRepository.findByUserIdAndEnhancementCardId(userId, cardId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.USER_ENHANCEMENT_CARD_NOT_FOUND,
                        "userId=" + userId + ", cardId=" + cardId));
    }

    /**
     * 유저 인벤 응답 (마스터 3종 기준, 미보유는 quantity=0).
     *
     * @param userId 유저 ID
     * @return 인벤 목록
     */
    public List<UserEnhancementCardResponse> getInventoryResponses(Long userId) {
        Map<Long, UserEnhancementCard> owned = userEnhancementCardRepository
                .findByUserIdOrderByEnhancementCardIdAsc(userId)
                .stream()
                .collect(Collectors.toMap(UserEnhancementCard::getEnhancementCardId, Function.identity()));

        return enhancementCardRepository.findAll().stream()
                .sorted(Comparator.comparing(c -> c.getEffect().ordinal()))
                .map(master -> {
                    int qty = owned.containsKey(master.getId())
                            ? owned.get(master.getId()).getQuantity()
                            : 0;
                    return new UserEnhancementCardResponse(
                            master.getId(), master.getName(), master.getEffect(), qty);
                })
                .toList();
    }
}
