package com.project.bluffball.domain.league.enums;

/**
 * 리그 티어(부) 구분.
 * DB에는 ordinal(0~6) 정수로 저장된다.
 *
 * <p>아마 1~4부, 독립, 프로 1~2부 — 총 7단계.</p>
 */
public enum LeagueTier {
    AMATEUR_1,      // 0 - 아마 1부
    AMATEUR_2,      // 1 - 아마 2부
    AMATEUR_3,      // 2 - 아마 3부
    AMATEUR_4,      // 3 - 아마 4부
    INDEPENDENT,    // 4 - 독립
    PRO_1,          // 5 - 프로 1부
    PRO_2           // 6 - 프로 2부
}
