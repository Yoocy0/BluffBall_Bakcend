package com.project.bluffball.gametest.bot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CompactRosterGapPlanner")
class CompactRosterGapPlannerTest {

    @Nested
    @DisplayName("기존 라인업")
    class KeepExisting {

        @Test
        @DisplayName("타자3+전담투수면 유지")
        void keep() {
            CompactRosterGapPlanner.Plan plan = CompactRosterGapPlanner.plan(
                    1L,
                    List.of(1L, 2L, 3L, 4L),
                    List.of(1L, 2L, 3L),
                    4L,
                    BotRole.BATTER);

            assertThat(plan.keepExisting()).isTrue();
            assertThat(plan.battersToCreate()).isZero();
            assertThat(plan.pitcherToCreate()).isZero();
            assertThat(plan.batterUserIds()).containsExactly(1L, 2L, 3L);
            assertThat(plan.pitcherUserId()).isEqualTo(4L);
        }

        @Test
        @DisplayName("투수가 타순에 있으면 무효 → 재구성")
        void pitcherInOrderInvalid() {
            CompactRosterGapPlanner.Plan plan = CompactRosterGapPlanner.plan(
                    1L,
                    List.of(1L, 2L, 3L),
                    List.of(1L, 2L, 3L),
                    1L,
                    BotRole.BATTER);

            assertThat(plan.keepExisting()).isFalse();
        }
    }

    @Nested
    @DisplayName("빈자리 계산")
    class Gaps {

        @Test
        @DisplayName("리더만 있고 타자면 타자봇2 + 투수봇1")
        void leaderBatterAlone() {
            CompactRosterGapPlanner.Plan plan = CompactRosterGapPlanner.plan(
                    1L,
                    List.of(1L),
                    List.of(),
                    null,
                    BotRole.BATTER);

            assertThat(plan.batterUserIds()).containsExactly(1L);
            assertThat(plan.pitcherUserId()).isNull();
            assertThat(plan.battersToCreate()).isEqualTo(2);
            assertThat(plan.pitcherToCreate()).isEqualTo(1);
        }

        @Test
        @DisplayName("리더만 있고 투수면 타자봇3")
        void leaderPitcherAlone() {
            CompactRosterGapPlanner.Plan plan = CompactRosterGapPlanner.plan(
                    1L,
                    List.of(1L),
                    List.of(),
                    null,
                    BotRole.PITCHER);

            assertThat(plan.batterUserIds()).isEmpty();
            assertThat(plan.pitcherUserId()).isEqualTo(1L);
            assertThat(plan.battersToCreate()).isEqualTo(3);
            assertThat(plan.pitcherToCreate()).isZero();
        }

        @Test
        @DisplayName("멤버가 있으면 봇 생성을 줄인다")
        void useExistingMembers() {
            CompactRosterGapPlanner.Plan plan = CompactRosterGapPlanner.plan(
                    1L,
                    List.of(1L, 2L, 3L, 4L),
                    List.of(),
                    null,
                    BotRole.BATTER);

            assertThat(plan.batterUserIds()).containsExactly(1L, 2L, 3L);
            assertThat(plan.pitcherUserId()).isEqualTo(4L);
            assertThat(plan.battersToCreate()).isZero();
            assertThat(plan.pitcherToCreate()).isZero();
        }
    }
}
