package com.project.bluffball.domain.team.enums;

/**
 * 팀 재정 거래 유형.
 * DB에는 ordinal(0~2) 정수로 저장된다.
 */
public enum TeamTreasuryTransactionType {
    DONATION,           // 0 - 팀원 기부
    SEASON_PRIZE,       // 1 - 시즌 성적 상금
    ENTRY_FEE_PAYMENT   // 2 - 리그 참여권 구매
}
