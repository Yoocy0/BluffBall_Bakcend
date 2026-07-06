package com.project.bluffball.domain.card.enums;

/**
 * 구종의 변화 방향.
 * DB에는 ordinal(0, 1, 2) 정수로 저장된다.
 */
public enum ChangeDirection {
    /** 아래 방향 (낙차) — changeAmount만큼 Y 증가 */
    DOWN,
    /** 옆 방향 (횡 변화) — changeAmount만큼 X 증가 (우측) */
    SIDE,
    /** 역 방향 (역회전·횡 변화) — changeAmount만큼 X 감소 (좌측) */
    REVERSE
}
