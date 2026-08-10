package com.project.bluffball.domain.card.service;

import com.project.bluffball.domain.card.dto.response.EnhancementCardResponse;
import com.project.bluffball.domain.card.dto.response.UserEnhancementCardResponse;
import com.project.bluffball.domain.card.service.usecase.executor.UserEnhancementCardExecutor;
import com.project.bluffball.domain.card.service.usecase.reader.EnhancementCardReader;
import com.project.bluffball.domain.card.service.usecase.validator.UserEnhancementCardValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 강화 카드 카탈로그·유저 보유 서비스.
 */
@Service
@RequiredArgsConstructor
public class EnhancementCardService {

    /** 강화 카드 Reader */
    private final EnhancementCardReader enhancementCardReader;

    /** 유저 강화 카드 Executor */
    private final UserEnhancementCardExecutor userEnhancementCardExecutor;

    /** 수량 검증 */
    private final UserEnhancementCardValidator userEnhancementCardValidator;

    /**
     * 강화 카드 마스터 목록을 반환한다.
     *
     * @return 카탈로그
     */
    public List<EnhancementCardResponse> getCatalog() {
        return enhancementCardReader.getCatalogResponses();
    }

    /**
     * 내 강화 카드 보유 목록을 반환한다.
     *
     * @param userId 유저 ID
     * @return 인벤 (미보유는 quantity=0)
     */
    public List<UserEnhancementCardResponse> getMyInventory(Long userId) {
        return enhancementCardReader.getInventoryResponses(userId);
    }

    /**
     * 강화 카드를 획득한다 (상점·테스트용).
     *
     * @param userId 유저 ID
     * @param cardId 마스터 ID
     * @param amount 수량
     * @return 갱신된 인벤 항목
     */
    public UserEnhancementCardResponse grant(Long userId, Long cardId, int amount) {
        userEnhancementCardExecutor.grant(userId, cardId, amount);
        return enhancementCardReader.getInventoryResponses(userId).stream()
                .filter(r -> r.cardId().equals(cardId))
                .findFirst()
                .orElseThrow();
    }

    /**
     * 강화 카드 1장 소모가 가능한지 검증만 한다 (적용 API에서 사용).
     *
     * @param userId 유저 ID
     * @param cardId 마스터 ID
     */
    public void requireConsumable(Long userId, Long cardId) {
        userEnhancementCardValidator.validateSufficientQuantity(
                enhancementCardReader.getQuantity(userId, cardId), 1);
    }
}
