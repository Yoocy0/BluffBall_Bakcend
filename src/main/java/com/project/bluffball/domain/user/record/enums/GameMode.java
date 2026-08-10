package com.project.bluffball.domain.user.record.enums;

/**
 * 게임 모드 구분.
 * DB에는 ordinal(0~4) 정수로 저장된다.
 *
 * <ul>
 *   <li>SHOWDOWN — 1 vs 1 쇼다운 (구 싱글/GENERAL)</li>
 *   <li>COMPACT_LEAGUE — 컴팩트 리그 (구 CLAN_MINI, 티어는 {@code league_id}로 구분)</li>
 *   <li>FULL_LEAGUE — 풀 리그 (구 CLAN_GENERAL, 티어는 {@code league_id}로 구분)</li>
 *   <li>CUSTOM — 커스텀 매치</li>
 *   <li>BOT — 연습용 봇 매치 (규칙은 SHOWDOWN과 동일: 1v1·핸드 3·1이닝·드로우/멀리건).
 *       별도 룰 엔진이 아니라 상대/연습 마커 + Showdown 규칙. 보상·래더 필터용.</li>
 * </ul>
 *
 * <p>새 상수는 반드시 맨 뒤에 추가한다. 기존 0~3 ordinal은 DB 호환을 위해 유지한다.</p>
 */
public enum GameMode {
    SHOWDOWN,           // 0 - 쇼다운 (1 vs 1)
    COMPACT_LEAGUE,     // 1 - 컴팩트 리그
    FULL_LEAGUE,        // 2 - 풀 리그
    CUSTOM,             // 3 - 커스텀 매치
    BOT;                // 4 - 연습용 봇 매치 (Showdown 규칙)

    /**
     * 연습용 봇 매치 모드인지.
     *
     * @return {@code BOT}이면 true
     */
    public boolean isBot() {
        return this == BOT;
    }

    /**
     * Showdown 계열 규칙(1v1·핸드 3·1이닝·드로우/멀리건)을 쓰는지.
     *
     * <p>SHOWDOWN / CUSTOM / BOT 이 해당한다.</p>
     *
     * @return Showdown 계열이면 true
     */
    public boolean usesShowdownRules() {
        return this == SHOWDOWN || this == CUSTOM || this == BOT;
    }
}
