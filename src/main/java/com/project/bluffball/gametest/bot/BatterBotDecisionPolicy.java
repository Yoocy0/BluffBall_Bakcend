package com.project.bluffball.gametest.bot;

import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.enums.Timing;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 타자 봇 스윙 판단 정책.
 *
 * <p>상대가 던진 구종을 기억하고, 스트라이크가 되지 않은 구종은 후보에서 뺀다.
 * 남은 구종에 대해 공개된 시작 좌표 기준 “존 진입 확률”이 가장 높은 구종을 가정한 뒤
 * 스윙(예상 최종 좌표·타이밍) 또는 지켜보기를 결정한다.</p>
 */
@Component("gameTestBatterBotDecisionPolicy")
public class BatterBotDecisionPolicy {

    /**
     * 시작 좌표가 공개됐을 때 스윙 여부를 결정한다.
     *
     * @param startCoordinateNumber PitcherReady 시작 좌표 (1~25)
     * @param catalogPitches 전체 구종 카탈로그
     * @param memory 구종 기억(제외 목록)
     * @return 스윙/지켜보기 결정
     */
    public BatterBotSwingDecision decide(
            int startCoordinateNumber,
            List<CardInfo> catalogPitches,
            BatterBotPitchMemory memory) {

        List<CardInfo> candidates = remainingCandidates(catalogPitches, memory);
        if (candidates.isEmpty()) {
            // 전부 제외되면 카탈로그 전체로 다시 판단
            candidates = new ArrayList<>(catalogPitches);
        }
        if (candidates.isEmpty()) {
            return BatterBotSwingDecision.take(null, 0.0);
        }

        CardInfo best = pickHighestProbability(startCoordinateNumber, candidates);
        int finalCoord = PitchTrajectory.finalCoordinate(startCoordinateNumber, best);
        double strikeProbability = PitchTrajectory.isStrikeZone(finalCoord) ? 1.0 : 0.0;

        if (strikeProbability >= 1.0 && finalCoord >= 1) {
            Timing timing = best.timing() != null ? best.timing() : Timing.NORMAL;
            return BatterBotSwingDecision.swing(
                    finalCoord, timing, best.name(), strikeProbability);
        }
        return BatterBotSwingDecision.take(best.name(), strikeProbability);
    }

    /**
     * 기억에서 제외되지 않은 구종만 남긴다.
     *
     * @param catalogPitches 카탈로그
     * @param memory 기억
     * @return 후보 목록
     */
    private List<CardInfo> remainingCandidates(
            List<CardInfo> catalogPitches,
            BatterBotPitchMemory memory) {
        List<CardInfo> remaining = new ArrayList<>();
        if (catalogPitches == null) {
            return remaining;
        }
        for (CardInfo pitch : catalogPitches) {
            if (pitch == null || pitch.name() == null) {
                continue;
            }
            if (memory == null || !memory.isEliminated(pitch.name())) {
                remaining.add(pitch);
            }
        }
        return remaining;
    }

    /**
     * 시작 좌표 기준 존 진입 확률이 가장 높은 구종을 고른다.
     *
     * <p>동점이면 변화량이 작은 구종 우선, 그래도 같으면 랜덤.</p>
     *
     * @param startCoordinateNumber 시작 좌표
     * @param candidates 후보
     * @return 최다확률 구종
     */
    private CardInfo pickHighestProbability(int startCoordinateNumber, List<CardInfo> candidates) {
        record Scored(CardInfo pitch, double score, int changeAmount) {
        }

        List<Scored> scored = new ArrayList<>(candidates.size());
        for (CardInfo pitch : candidates) {
            int finalCoord = PitchTrajectory.finalCoordinate(startCoordinateNumber, pitch);
            double score = PitchTrajectory.isStrikeZone(finalCoord) ? 1.0 : 0.0;
            scored.add(new Scored(pitch, score, pitch.changeAmount()));
        }

        double bestScore = scored.stream()
                .mapToDouble(Scored::score)
                .max()
                .orElse(0.0);

        List<Scored> top = scored.stream()
                .filter(s -> s.score() == bestScore)
                .sorted(Comparator.comparingInt(Scored::changeAmount))
                .toList();

        int minChange = top.get(0).changeAmount();
        List<Scored> tightest = top.stream()
                .filter(s -> s.changeAmount() == minChange)
                .toList();

        int index = ThreadLocalRandom.current().nextInt(tightest.size());
        return tightest.get(index).pitch();
    }
}
