package com.project.bluffball.domain.user.record.enums;

/**
 * 게임 모드 구분.
 * DB에는 ordinal(0~3) 정수로 저장된다.
 *
 * <ul>
 *   <li>SHOWDOWN — 1 vs 1 쇼다운 (구 싱글/GENERAL)</li>
 *   <li>COMPACT_LEAGUE — 컴팩트 리그 (구 CLAN_MINI, 티어는 {@code league_id}로 구분)</li>
 *   <li>FULL_LEAGUE — 풀 리그 (구 CLAN_GENERAL, 티어는 {@code league_id}로 구분)</li>
 *   <li>CUSTOM — 커스텀 매치</li>
 * </ul>
 */
public enum GameMode {
    SHOWDOWN,           // 0 - 쇼다운 (1 vs 1)
    COMPACT_LEAGUE,     // 1 - 컴팩트 리그
    FULL_LEAGUE,        // 2 - 풀 리그
    CUSTOM              // 3 - 커스텀 매치
}
