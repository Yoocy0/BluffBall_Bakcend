package com.project.bluffball.gametest.bot;

import com.project.bluffball.domain.card.enums.ChangeDirection;
import com.project.bluffball.domain.game.dto.response.CardInfo;

/**
 * 구종 궤적(시작 좌표 → 최종 좌표) 계산.
 *
 * <p>{@link com.project.bluffball.domain.card.entity.PitchCard}와 동일한 격자 규칙을 쓴다.</p>
 */
final class PitchTrajectory {

    private PitchTrajectory() {
    }

    /**
     * 시작 좌표에 구종 변화를 적용한 최종 좌표를 계산한다.
     *
     * @param startCoordinateNumber 시작 좌표 (1~25)
     * @param pitch 구종
     * @return 최종 좌표 (1~25), 격자 이탈 시 0
     */
    static int finalCoordinate(int startCoordinateNumber, CardInfo pitch) {
        return finalCoordinate(
                startCoordinateNumber,
                pitch.changeAmount(),
                pitch.direction());
    }

    /**
     * 시작 좌표에 변화량·방향을 적용한다.
     *
     * @param startCoordinateNumber 시작 좌표
     * @param changeAmount 변화량
     * @param direction 방향
     * @return 최종 좌표 또는 0
     */
    static int finalCoordinate(
            int startCoordinateNumber,
            int changeAmount,
            ChangeDirection direction) {
        if (startCoordinateNumber < 1 || startCoordinateNumber > 25) {
            return 0;
        }
        int startY = (startCoordinateNumber - 1) / 5;
        int startX = (startCoordinateNumber - 1) % 5;
        int finalX = startX;
        int finalY = startY;

        switch (direction) {
            case SIDE -> {
                finalX = startX + changeAmount;
                if (finalX > 4) {
                    return 0;
                }
            }
            case REVERSE -> {
                finalX = startX - changeAmount;
                if (finalX < 0) {
                    return 0;
                }
            }
            case DOWN -> {
                finalY = startY + changeAmount;
                if (finalY > 4) {
                    return 0;
                }
            }
        }
        return finalY * 5 + finalX + 1;
    }

    /**
     * 스트라이크존 여부 (중앙 3×3: 7–9, 12–14, 17–19).
     *
     * @param coordinateNumber 좌표
     * @return 존이면 true
     */
    static boolean isStrikeZone(int coordinateNumber) {
        return switch (coordinateNumber) {
            case 7, 8, 9, 12, 13, 14, 17, 18, 19 -> true;
            default -> false;
        };
    }
}
