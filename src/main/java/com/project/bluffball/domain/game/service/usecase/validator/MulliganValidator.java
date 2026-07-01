package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/**
 * 멀리건(카드 교체) 요청 검증 컴포넌트.
 */
@Component
public class MulliganValidator {

    /** 멀리건 요청 가능 여부 — 해당 유저가 이미 완료했으면 거부 */
    public void validateMulliganAllowedForUser(boolean mulliganDoneForUser) {
        if (mulliganDoneForUser) {
            throw new IllegalStateException("멀리건은 등판 당 1회만 가능합니다.");
        }
    }

    /** 요청 유저가 매치 참가자인지 검증 */
    public void validateParticipant(MatchInfoReader matchInfoReader,
                                    String matchSessionId,
                                    Long requestUserId) {
        if (!matchInfoReader.isParticipant(matchSessionId, requestUserId)) {
            throw new IllegalArgumentException("매치 참가자만 멀리건을 진행할 수 있습니다.");
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
