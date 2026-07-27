package com.project.bluffball.gametest.bot;

import com.project.bluffball.domain.game.enums.Timing;

/**
 * 타자 봇의 한 투구에 대한 스윙/지켜보기 결정.
 *
 * @param swing 스윙 여부 (false면 지켜보기, 좌표 0)
 * @param batterCoordinateNumber 스윙 시 예상 최종 좌표 (1~25), 지켜보기면 0
 * @param timing 스윙 시 맞출 타이밍, 지켜보기면 {@link Timing#NORMAL}
 * @param assumedPitchName 판단에 쓴 최다확률 구종명 (디버그용)
 * @param strikeProbability 그 구종이 존에 들어갈 확률(0 또는 1 기반 점수)
 */
public record BatterBotSwingDecision(
        boolean swing,
        int batterCoordinateNumber,
        Timing timing,
        String assumedPitchName,
        double strikeProbability
) {

    /**
     * 지켜보기 결정을 만든다.
     *
     * @param assumedPitchName 가정 구종
     * @param strikeProbability 그 구종의 존 확률
     * @return 지켜보기
     */
    public static BatterBotSwingDecision take(String assumedPitchName, double strikeProbability) {
        return new BatterBotSwingDecision(
                false, 0, Timing.NORMAL, assumedPitchName, strikeProbability);
    }

    /**
     * 스윙 결정을 만든다.
     *
     * @param coordinate 최종 좌표
     * @param timing 타이밍
     * @param assumedPitchName 가정 구종
     * @param strikeProbability 존 확률
     * @return 스윙
     */
    public static BatterBotSwingDecision swing(
            int coordinate,
            Timing timing,
            String assumedPitchName,
            double strikeProbability) {
        return new BatterBotSwingDecision(
                true, coordinate, timing, assumedPitchName, strikeProbability);
    }
}
