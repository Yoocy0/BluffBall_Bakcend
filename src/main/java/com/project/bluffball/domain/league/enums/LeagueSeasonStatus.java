package com.project.bluffball.domain.league.enums;

/**
 * 리그 시즌 진행 상태.
 * DB에는 ordinal(0~2) 정수로 저장된다.
 */
public enum LeagueSeasonStatus {
    RECRUITING,     // 0 - 모집 중 (참여권 구매·팀 등록)
    IN_PROGRESS,    // 1 - 진행 중
    ENDED           // 2 - 종료
}
