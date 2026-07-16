package com.project.bluffball.domain.league.enums;

/**
 * 리그 형식 구분.
 * DB에는 ordinal(0=FULL, 1=COMPACT) 정수로 저장된다.
 */
public enum LeagueFormat {
    FULL,       // 0 - 풀 리그
    COMPACT     // 1 - 컴팩트 리그
}
