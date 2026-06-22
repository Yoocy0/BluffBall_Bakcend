package com.project.bluffball.domain.game.service.usecase.validator;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/**
 * 멀리건(카드 교체) 요청 검증 컴포넌트.
 *
 * <h3>검증 규칙</h3>
 * <ul>
 *   <li>멀리건이 아직 완료되지 않았을 것 (등판 당 1회)</li>
 *   <li>교체 목록(cardIdsToSwap)에 중복된 카드 ID가 없을 것</li>
 *   <li>교체 목록의 모든 카드 ID가 현재 패(currentHand)에 포함될 것</li>
 *   <li>교체 목록의 크기가 현재 패 크기를 초과하지 않을 것 (전체 교체는 허용)</li>
 * </ul>
 */
@Component
public class MulliganValidator {

    /** 멀리건 요청 가능 여부 — {@code mulliganDone == true}이면 거부 */
    public void validateMulliganAllowed(boolean mulliganDone) {
        if (mulliganDone) {
            throw new IllegalStateException("멀리건은 투수 등판 당 1회만 가능합니다.");
        }
    }

    public void validate(List<Long> currentHand, List<Long> cardIdsToSwap) {
        if (cardIdsToSwap == null || cardIdsToSwap.isEmpty()) {
            return;
        }

        if (cardIdsToSwap.size() > currentHand.size()) {
            throw new IllegalArgumentException(
                    "교체 카드 수(" + cardIdsToSwap.size() + ")가 현재 패 크기(" + currentHand.size() + ")를 초과합니다.");
        }

        if (new HashSet<>(cardIdsToSwap).size() != cardIdsToSwap.size()) {
            throw new IllegalArgumentException("교체 목록에 중복된 카드 ID가 있습니다.");
        }

        HashSet<Long> handSet = new HashSet<>(currentHand);
        for (Long id : cardIdsToSwap) {
            if (!handSet.contains(id)) {
                throw new IllegalArgumentException(
                        "교체하려는 카드가 현재 패에 없습니다. cardId=" + id);
            }
        }
    }
}
