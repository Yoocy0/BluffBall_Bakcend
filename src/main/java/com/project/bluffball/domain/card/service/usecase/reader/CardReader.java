package com.project.bluffball.domain.card.service.usecase.reader;

import com.project.bluffball.domain.card.entity.Card;
import com.project.bluffball.domain.card.entity.CoordinateCard;
import com.project.bluffball.domain.card.entity.PitchCard;
import com.project.bluffball.domain.card.enums.UserType;
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
     * 리그 사전 선택(구종·강화)으로 허용되는 카드인지 판정한다.
     *
     * <p>허용: {@link PitchCard}, 또는 기본 Card 중 {@code userType=PITCHER}(구종 강화).
     * 좌표 카드·타자 타이밍 카드는 불가.</p>
     *
     * @param cardIds 카드 ID 목록
     * @return 전부 허용 카드이면 true
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
        return cards.stream().allMatch(this::isSelectablePitchHandCard);
    }

    /**
     * 단일 카드가 구종 핸드로 선택 가능한지 판정한다.
     *
     * @param card 카드 Entity
     * @return 선택 가능하면 true
     */
    private boolean isSelectablePitchHandCard(Card card) {
        if (card instanceof CoordinateCard) {
            return false;
        }
        if (card instanceof PitchCard) {
            return true;
        }
        return card.getUserType() == UserType.PITCHER;
    }
}
