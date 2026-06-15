package com.project.bluffball.domain.user.record.enums;

/**
 * 게임 모드 구분.
 * DB에는 ordinal(0, 1, 2) 정수로 저장된다.
 */
public enum GameMode {
    GENERAL,    // 0 - 일반 모드
    CLAN_MINI,       // 1 - 클랜 친선 모드
    CLAN_GENERAL        // 2 - 클랜 정규 모드
}
