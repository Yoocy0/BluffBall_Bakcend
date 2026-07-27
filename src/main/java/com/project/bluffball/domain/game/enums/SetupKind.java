package com.project.bluffball.domain.game.enums;

/**
 * 블러핑 숫자(셋업) 제출 종류.
 *
 * <p>쇼다운은 {@link #FULL}(4종 일괄).
 * 리그는 역할별 {@link #PITCHER}(아웃·병살) / {@link #BATTER}(3루타·홈런).</p>
 */
public enum SetupKind {
    /** 쇼다운 — OUT·병살·3루타·홈런 일괄 */
    FULL,
    /** 리그 투수 — OUT·병살 */
    PITCHER,
    /** 리그 타자 — 3루타·홈런 */
    BATTER
}
