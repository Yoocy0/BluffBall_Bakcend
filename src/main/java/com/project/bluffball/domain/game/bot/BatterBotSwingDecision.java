package com.project.bluffball.domain.game.bot;

import com.project.bluffball.domain.game.enums.Timing;

/**
 * 타자 봇의 한 투구에 대한 스윙/지켜보기 결정.
 *
 * @param swing                  스윙 여부 (false면 지켜보기, 좌표 0)
 * @param batterCoordinateNumber 스윙 시 예상 최종 좌표 (1~25), 지켜보기면 0
 * @param timing                 스윙 시 맞출 타이밍, 지켜보기면 {@link Timing#NORMAL}
 * @param assumedPitchName       가정한 구종명 (로그용)
 */
public record BatterBotSwingDecision(
        boolean swing,
        int batterCoordinateNumber,
        Timing timing,
        String assumedPitchName
) {

    /**
     * 지켜보기(좌표 0) 결정을 만든다.
     *
     * @param assumedPitchName 가정 구종
     * @return 지켜보기
     */
    public static BatterBotSwingDecision take(String assumedPitchName) {
        return new BatterBotSwingDecision(false, 0, Timing.NORMAL, assumedPitchName);
    }

    /**
     * 스윙 결정을 만든다.
     *
     * @param coordinate       예상 최종 좌표
     * @param timing           타이밍
     * @param assumedPitchName 가정 구종
     * @return 스윙
     */
    public static BatterBotSwingDecision swing(
            int coordinate,
            Timing timing,
            String assumedPitchName) {
        Timing resolved = timing != null ? timing : Timing.NORMAL;
        return new BatterBotSwingDecision(true, coordinate, resolved, assumedPitchName);
    }
}
