package com.project.bluffball.domain.league.config;

import com.project.bluffball.domain.league.enums.LeagueTier;

import java.util.List;

/**
 * 티어별 점수 밴드·승급/강등 기준.
 *
 * <p>사다리(낮→높): 아마4 → 아마3 → 아마2 → 아마1 → 독립 → 프로1 → 프로2.
 * 티어 내 점수는 {@code [floor, ceil]}로 클램프되며, {@code ceil} 도달 시 상위 진출 자격이 생긴다.</p>
 */
public final class LeagueTierRule {

    /** 승급 사다리 (인덱스 0 = 최하위) */
    public static final List<LeagueTier> LADDER = List.of(
            LeagueTier.AMATEUR_4,
            LeagueTier.AMATEUR_3,
            LeagueTier.AMATEUR_2,
            LeagueTier.AMATEUR_1,
            LeagueTier.INDEPENDENT,
            LeagueTier.PRO_1,
            LeagueTier.PRO_2
    );

    private final LeagueTier tier;
    private final int floor;
    private final int ceil;
    /** 강등 기준 점수 (미만이면 강등). 최하위는 null */
    private final Integer demoteBelow;

    private LeagueTierRule(LeagueTier tier, int floor, int ceil, Integer demoteBelow) {
        this.tier = tier;
        this.floor = floor;
        this.ceil = ceil;
        this.demoteBelow = demoteBelow;
    }

    /**
     * 티어 규칙을 반환한다.
     *
     * @param tier 티어
     * @return 규칙
     */
    public static LeagueTierRule of(LeagueTier tier) {
        return switch (tier) {
            case AMATEUR_4 -> new LeagueTierRule(tier, 0, 150, null);
            case AMATEUR_3 -> new LeagueTierRule(tier, 150, 300, 150);
            case AMATEUR_2 -> new LeagueTierRule(tier, 300, 450, 300);
            case AMATEUR_1 -> new LeagueTierRule(tier, 450, 600, 450);
            case INDEPENDENT -> new LeagueTierRule(tier, 600, 750, 600);
            case PRO_1 -> new LeagueTierRule(tier, 750, 900, 750);
            case PRO_2 -> new LeagueTierRule(tier, 900, 1050, 900);
        };
    }

    /**
     * @return 티어
     */
    public LeagueTier tier() {
        return tier;
    }

    /**
     * @return 점수 하한
     */
    public int floor() {
        return floor;
    }

    /**
     * @return 점수 상한(=진출 자격 점수)
     */
    public int ceil() {
        return ceil;
    }

    /**
     * @return 강등 기준(미만), 최하위면 null
     */
    public Integer demoteBelow() {
        return demoteBelow;
    }

    /**
     * 상위 티어 진출 자격 점수에 도달했는지.
     *
     * @param rating 현재 점수
     * @return 자격 있으면 true
     */
    public boolean isPromoteReady(int rating) {
        return rating >= ceil && nextTier(tier) != null;
    }

    /**
     * 강등 대상인지.
     *
     * @param rating 현재 점수
     * @return 강등이면 true
     */
    public boolean shouldDemote(int rating) {
        return demoteBelow != null && rating < demoteBelow;
    }

    /**
     * 점수를 티어 상한으로 클램프한다.
     *
     * <p>하한(floor) 미만은 허용한다. 강등 판정은 주기 배치에서 {@link #shouldDemote(int)}로 한다.</p>
     *
     * @param rating 원점수
     * @return 상한 클램프 점수 (0 미만은 0)
     */
    public int clamp(int rating) {
        return Math.max(0, Math.min(ceil, rating));
    }

    /**
     * 사다리상 다음(상위) 티어.
     *
     * @param tier 현재
     * @return 상위 티어 또는 null
     */
    public static LeagueTier nextTier(LeagueTier tier) {
        int idx = LADDER.indexOf(tier);
        if (idx < 0 || idx >= LADDER.size() - 1) {
            return null;
        }
        return LADDER.get(idx + 1);
    }

    /**
     * 사다리상 이전(하위) 티어.
     *
     * @param tier 현재
     * @return 하위 티어 또는 null
     */
    public static LeagueTier previousTier(LeagueTier tier) {
        int idx = LADDER.indexOf(tier);
        if (idx <= 0) {
            return null;
        }
        return LADDER.get(idx - 1);
    }

    /**
     * 최하위 티어.
     *
     * @return 아마 4부
     */
    public static LeagueTier lowestTier() {
        return LADDER.get(0);
    }
}
