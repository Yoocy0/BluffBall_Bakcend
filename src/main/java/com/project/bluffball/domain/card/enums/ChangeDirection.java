package com.project.bluffball.domain.card.enums;

/**
 * 구종의 변화 방향.
 * DB에는 ordinal(0, 1, 2) 정수로 저장된다.
 */
public enum ChangeDirection {
    NONE,   // 0 - 변화 없음 (직구)
    DOWN,   // 1 - 아래 방향 (낙차)
    SIDE    // 2 - 옆 방향 (횡변화)
}
