package com.project.bluffball.gametest.bot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Full 로스터 빈자리 계산 (타순 9명, 선발 투수는 타순 포함).
 *
 * <p>기존 라인업이 유효하면 유지하고, 아니면 리더·기존 멤버로 채운 뒤
 * 부족한 타자 수(생성할 봇 수)만 반환한다.</p>
 */
public final class FullRosterGapPlanner {

    /** Full 타순 인원 */
    public static final int BATTING_ORDER_SIZE = 9;

    private FullRosterGapPlanner() {
    }

    /**
     * 빈자리 채움 계획을 계산한다.
     *
     * @param leaderUserId 리더(사람) userId
     * @param memberUserIds 현재 팀 멤버 (리더 포함)
     * @param existingBatters 기존 타순 (없으면 empty)
     * @param existingPitcher 기존 선발 투수 (없으면 null)
     * @param leaderRole 라인업을 새로 짤 때 리더 역할 (투수면 선발로 지정)
     * @return 계획
     */
    public static Plan plan(
            Long leaderUserId,
            List<Long> memberUserIds,
            List<Long> existingBatters,
            Long existingPitcher,
            BotRole leaderRole) {

        Objects.requireNonNull(leaderUserId, "leaderUserId");
        Objects.requireNonNull(leaderRole, "leaderRole");

        if (isValidFullLineup(existingBatters, existingPitcher)) {
            return Plan.keep(List.copyOf(existingBatters), existingPitcher);
        }

        List<Long> batters = new ArrayList<>(BATTING_ORDER_SIZE);
        batters.add(leaderUserId);

        Set<Long> used = new LinkedHashSet<>();
        used.add(leaderUserId);

        List<Long> others = memberUserIds == null ? List.of() : memberUserIds.stream()
                .filter(id -> id != null && !id.equals(leaderUserId))
                .toList();

        for (Long memberId : others) {
            if (batters.size() >= BATTING_ORDER_SIZE) {
                break;
            }
            batters.add(memberId);
            used.add(memberId);
        }

        int battersToCreate = BATTING_ORDER_SIZE - batters.size();

        Long pitcher;
        if (leaderRole == BotRole.PITCHER) {
            pitcher = leaderUserId;
        } else if (batters.size() > 1) {
            pitcher = batters.get(1);
        } else {
            // 봇 생성 후 서비스에서 마지막 타자(봇)를 선발로 지정
            pitcher = null;
        }

        return Plan.needsFill(List.copyOf(batters), pitcher, battersToCreate, List.copyOf(used));
    }

    /**
     * Full 라인업이 유효한지 (타순 9 + 선발이 타순에 포함).
     *
     * @param batters 타순
     * @param pitcher 선발 투수
     * @return 유효하면 true
     */
    public static boolean isValidFullLineup(List<Long> batters, Long pitcher) {
        if (batters == null || batters.size() != BATTING_ORDER_SIZE || pitcher == null) {
            return false;
        }
        Set<Long> unique = new LinkedHashSet<>(batters);
        if (unique.size() != BATTING_ORDER_SIZE) {
            return false;
        }
        return unique.contains(pitcher);
    }

    /**
     * 빈자리 채움 계획.
     *
     * @param batterUserIds 이미 확정된 타자 (부족할 수 있음)
     * @param pitcherUserId 확정 선발 (없으면 null — 봇 생성 후 지정)
     * @param battersToCreate 생성할 타자 봇 수
     * @param alreadyAssigned 이미 역할에 쓴 멤버
     * @param keepExisting 기존 라인업 유지 여부
     */
    public record Plan(
            List<Long> batterUserIds,
            Long pitcherUserId,
            int battersToCreate,
            List<Long> alreadyAssigned,
            boolean keepExisting
    ) {
        /**
         * 기존 라인업 유지.
         *
         * @param batters 타순
         * @param pitcher 선발
         * @return 계획
         */
        public static Plan keep(List<Long> batters, Long pitcher) {
            return new Plan(batters, pitcher, 0, List.copyOf(batters), true);
        }

        /**
         * 봇 생성이 필요한 계획.
         *
         * @param batters 확정 타자
         * @param pitcher 확정 선발(nullable)
         * @param battersToCreate 타자 봇 수
         * @param alreadyAssigned 사용 중 멤버
         * @return 계획
         */
        public static Plan needsFill(
                List<Long> batters,
                Long pitcher,
                int battersToCreate,
                List<Long> alreadyAssigned) {
            return new Plan(batters, pitcher, battersToCreate, alreadyAssigned, false);
        }
    }
}
