package com.project.bluffball.domain.user.record.enums;

/**
 * 게임 모드 구분.
 * DB에는 ordinal(0, 1, 2) 정수로 저장된다.
 */
public enum GameMode {
    GENERAL,        // 0 - 싱글 모드 (1 vs 1 / 1이닝)
    CLAN_MINI,      // 1 - 클랜전 미니 (4 vs 4 / 3이닝)
    CLAN_GENERAL,   // 2 - 클랜전 정규 (10 vs 10 / 9이닝)
    CUSTOM          // 3 - 커스텀 매치 (자유 설정)
}
