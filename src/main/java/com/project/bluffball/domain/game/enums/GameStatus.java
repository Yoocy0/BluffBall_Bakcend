package com.project.bluffball.domain.game.enums;

/**
 * 매치 진행 상태.
 * Redis에는 ordinal(0, 1, 2) 정수로 저장된다.
 * value는 ordinal과 항상 일치해야 한다.
 */
public enum GameStatus {
    WAITING(0),     // 플레이어 입장 대기 중
    PLAYING(1),     // 경기 진행 중
    FINISHED(2);    // 경기 종료

    private final int value;

    GameStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
