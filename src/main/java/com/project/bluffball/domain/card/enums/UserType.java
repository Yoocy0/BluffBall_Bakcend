package com.project.bluffball.domain.card.enums;

/**
 * 카드를 사용할 수 있는 플레이어 유형.
 * DB에는 ordinal(0, 1, 2) 정수로 저장된다.
 */
public enum UserType {
    PITCHER,    // 0 - 투수 전용
    BATTER,     // 1 - 타자 전용
    BOTH        // 2 - 공용
}
