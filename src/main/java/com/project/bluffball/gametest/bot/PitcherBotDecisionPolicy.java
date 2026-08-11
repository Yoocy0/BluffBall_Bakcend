package com.project.bluffball.gametest.bot;

import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.gametest.bot.PitcherBotThrowDecision.Intent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 투수 봇 투구 판단 정책.
 *
 * <p>핸드에 있는 구종만 사용한다. 카운트에 따라 직구형(승부)/유인구형을 고르고,
 * 시작 좌표는 의도(존 스트라이크 / 존에서 빠짐)에 맞게 고른다.</p>
 *
 * <ul>
 *   <li>초구·불리(볼 ≥ 스트라이크) — 직구형 + 존 스트라이크</li>
 *   <li>유리(스트라이크 &gt; 볼) — 유인구형 + 존 출발 후 빠짐</li>
 *   <li>해당 타입이 핸드에 없으면 차선 구종으로 같은 의도 유지</li>
 * </ul>
 */
@Component("gameTestPitcherBotDecisionPolicy")
public class PitcherBotDecisionPolicy {

    /** 스트라이크존 후보 시작 좌표 (중앙 우선) */
    private static final int[] ZONE_STARTS = {13, 8, 12, 14, 18, 7, 9, 17, 19};

    /**
     * 현재 핸드·카운트로 투구를 결정한다.
     *
     * @param hand 투수 핸드 구종
     * @param balls 볼 카운트 (0~3)
     * @param strikes 스트라이크 카운트 (0~2)
     * @return 구종·시작 좌표
     * @throws IllegalArgumentException 핸드가 비어 있으면
     */
    public PitcherBotThrowDecision decide(List<CardInfo> hand, int balls, int strikes) {
        if (hand == null || hand.isEmpty()) {
            throw new IllegalArgumentException("pitcher hand is empty");
        }

        Intent intent = resolveIntent(balls, strikes);
        CardInfo pitch = pickPitch(hand, intent);
        int start = pickStartCoordinate(pitch, intent);

        return new PitcherBotThrowDecision(
                pitch.cardId(),
                start,
                pitch.name(),
                intent);
    }

    /**
     * 카운트로 투구 의도를 정한다.
     *
     * @param balls 볼
     * @param strikes 스트라이크
     * @return CHALLENGE 또는 BAIT
     */
    Intent resolveIntent(int balls, int strikes) {
        // 유리: 스트라이크가 더 많으면 유인구
        if (strikes > balls) {
            return Intent.BAIT;
        }
        // 초구·동점·불리: 존 승부
        return Intent.CHALLENGE;
    }

    /**
     * 의도에 맞는 구종을 핸드에서 고른다.
     *
     * @param hand 핸드
     * @param intent 의도
     * @return 선택된 구종
     */
    private CardInfo pickPitch(List<CardInfo> hand, Intent intent) {
        List<CardInfo> preferred = hand.stream()
                .filter(p -> intent == Intent.BAIT ? isBaitPitch(p) : isChallengePitch(p))
                .toList();

        List<CardInfo> pool = preferred.isEmpty() ? hand : preferred;
        return pickFromPool(pool, intent);
    }

    /**
     * 풀에서 하나를 고른다. 승부는 변화량 작은 것, 유인구는 변화량 큰 것 우선.
     *
     * @param pool 후보
     * @param intent 의도
     * @return 구종
     */
    private CardInfo pickFromPool(List<CardInfo> pool, Intent intent) {
        Comparator<CardInfo> byChange = Comparator.comparingInt(CardInfo::changeAmount);
        List<CardInfo> ordered = new ArrayList<>(pool);
        if (intent == Intent.BAIT) {
            ordered.sort(byChange.reversed());
        } else {
            ordered.sort(byChange);
        }

        int bestChange = ordered.get(0).changeAmount();
        List<CardInfo> tight = ordered.stream()
                .filter(p -> p.changeAmount() == bestChange)
                .toList();
        return tight.get(ThreadLocalRandom.current().nextInt(tight.size()));
    }

    /**
     * 직구형(승부) 구종인지.
     *
     * <p>변화량 0~1 또는 EARLY 타이밍.</p>
     *
     * @param pitch 구종
     * @return 직구형이면 true
     */
    boolean isChallengePitch(CardInfo pitch) {
        if (pitch.changeAmount() <= 1) {
            return true;
        }
        return pitch.timing() == Timing.EARLY;
    }

    /**
     * 유인구형 구종인지.
     *
     * <p>변화량 2 이상.</p>
     *
     * @param pitch 구종
     * @return 유인구형이면 true
     */
    boolean isBaitPitch(CardInfo pitch) {
        return pitch.changeAmount() >= 2;
    }

    /**
     * 의도에 맞는 시작 좌표를 고른다.
     *
     * @param pitch 구종
     * @param intent 의도
     * @return 시작 좌표 1~25
     */
    private int pickStartCoordinate(CardInfo pitch, Intent intent) {
        if (intent == Intent.CHALLENGE) {
            return pickChallengeStart(pitch);
        }
        return pickBaitStart(pitch);
    }

    /**
     * 최종 좌표가 존에 들어오는 시작 좌표를 고른다.
     *
     * @param pitch 구종
     * @return 시작 좌표
     */
    private int pickChallengeStart(CardInfo pitch) {
        List<Integer> good = new ArrayList<>();
        for (int start : ZONE_STARTS) {
            int fin = PitchTrajectory.finalCoordinate(start, pitch);
            if (PitchTrajectory.isStrikeZone(fin)) {
                good.add(start);
            }
        }
        if (!good.isEmpty()) {
            return good.get(ThreadLocalRandom.current().nextInt(good.size()));
        }
        // 존 시작이라도 확보
        return ZONE_STARTS[ThreadLocalRandom.current().nextInt(ZONE_STARTS.length)];
    }

    /**
     * 존에서 시작해 최종이 존 밖(또는 0)으로 빠지는 시작 좌표를 고른다.
     *
     * @param pitch 구종
     * @return 시작 좌표
     */
    private int pickBaitStart(CardInfo pitch) {
        List<Integer> baitStarts = new ArrayList<>();
        for (int start : ZONE_STARTS) {
            int fin = PitchTrajectory.finalCoordinate(start, pitch);
            if (!PitchTrajectory.isStrikeZone(fin)) {
                baitStarts.add(start);
            }
        }
        if (!baitStarts.isEmpty()) {
            return baitStarts.get(ThreadLocalRandom.current().nextInt(baitStarts.size()));
        }
        // 빠져나갈 시작이 없으면 존 가장자리로라도
        return ZONE_STARTS[ThreadLocalRandom.current().nextInt(ZONE_STARTS.length)];
    }
}
