package com.project.bluffball.domain.game.service.usecase.reader;

import com.project.bluffball.domain.card.entity.PitchCard;
import com.project.bluffball.domain.card.repository.PitchCardRepository;
import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.enums.Timing;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 구종 카드(PitchCard) DB 조회 전담 리더.
 *
 * <h3>메서드 호출 가능 레이어</h3>
 * <ul>
 *   <li>{@link #findAllIds} — Executor·Reader 내부, Service ✅ (ID 반환)</li>
 *   <li>{@link #getPitchCardDetails} — Service ✅ (DTO 반환)</li>
 *   <li>{@link #getById} — Executor·Reader 내부 전용 (Entity 반환)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PitchCardReader {

    private final PitchCardRepository pitchCardRepository;

    /**
     * 전체 구종 카드 ID 목록 반환 — 뽑기 풀(pool)로 사용된다.
     */
    public List<Long> findAllIds() {
        return pitchCardRepository.findAllIds();
    }

    /**
     * 지정된 ID 목록의 카드를 {@link CardInfo} DTO 목록으로 반환한다. (Service ✅)
     *
     * <p>Entity를 내부에서 조회·변환하므로 Service는 Entity를 알 필요가 없다.</p>
     */
    public List<CardInfo> getPitchCardDetails(List<Long> ids) {
        return pitchCardRepository.findAllByIds(ids).stream()
                .map(c -> new CardInfo(
                        c.getId(),
                        c.getName(),
                        c.getChangeAmount(),
                        c.getDirection(),
                        c.getTiming()))
                .collect(Collectors.toList());
    }

    /** 구종 카드명 조회 — Service 전용 */
    public String getPitchCardName(Long cardId) {
        if (cardId == null) {
            return null;
        }
        return getById(cardId).getName();
    }

    /** Executor·Reader 내부 전용 — Service에서 호출 금지 */
    public PitchCard getById(Long cardId) {
        return pitchCardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "구종 카드를 찾을 수 없습니다. cardId=" + cardId));
    }

    /**
     * 시작 좌표에 구종 변화를 적용한 최종 좌표 번호를 반환한다. (Executor·Reader 내부)
     */
    public int calculateFinalCoordinateNumber(Long pitchCardId, int startCoordinateNumber) {
        return getById(pitchCardId).calculateFinalCoordinateNumber(startCoordinateNumber);
    }

    /** 구종 카드의 고유 타이밍을 반환한다. (Executor·Reader 내부) */
    public Timing getPitchTiming(Long pitchCardId) {
        return getById(pitchCardId).getTiming();
    }
}
