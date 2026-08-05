package com.project.bluffball.domain.card.service.usecase.reader;

import com.project.bluffball.domain.card.entity.Card;
import com.project.bluffball.domain.card.entity.PitchCard;
import com.project.bluffball.domain.card.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 카드 읽기 전담 Reader (usecase/reader 계층).
 */
@Component
@RequiredArgsConstructor
public class CardReader {

    private final CardRepository cardRepository;

    /**
     * 리그 사전 선택으로 허용되는 구종 카드인지 판정한다.
     *
     * <p>허용: {@link PitchCard}만. 좌표·타자 타이밍·구 강화 카드 타입은 불가.
     * 강화는 {@code UserPitchCard} 오버레이로 처리한다.</p>
     *
     * @param cardIds 카드 ID 목록
     * @return 전부 구종 카드이면 true
     */
    public boolean areValidPitcherHandCards(List<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            return false;
        }
        Set<Long> uniqueIds = new HashSet<>(cardIds);
        if (uniqueIds.size() != cardIds.size()) {
            return false;
        }
        List<Card> cards = cardRepository.findAllById(uniqueIds);
        if (cards.size() != uniqueIds.size()) {
            return false;
        }
        return cards.stream().allMatch(card -> card instanceof PitchCard);
    }
}
