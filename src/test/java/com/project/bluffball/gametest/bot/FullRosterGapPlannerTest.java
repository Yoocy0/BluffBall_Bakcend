package com.project.bluffball.gametest.bot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FullRosterGapPlanner")
class FullRosterGapPlannerTest {

    @Nested
    @DisplayName("기존 라인업")
    class KeepExisting {

        @Test
        @DisplayName("타자9+선발이 타순에 있으면 유지")
        void keep() {
            List<Long> batters = LongStream.rangeClosed(1, 9).boxed().toList();
            FullRosterGapPlanner.Plan plan = FullRosterGapPlanner.plan(
                    1L, batters, batters, 5L, BotRole.BATTER);

            assertThat(plan.keepExisting()).isTrue();
            assertThat(plan.battersToCreate()).isZero();
            assertThat(plan.batterUserIds()).hasSize(9);
            assertThat(plan.pitcherUserId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("선발이 타순 밖이면 무효 → 재구성")
        void pitcherOutsideInvalid() {
            List<Long> batters = LongStream.rangeClosed(1, 9).boxed().toList();
            FullRosterGapPlanner.Plan plan = FullRosterGapPlanner.plan(
                    1L, batters, batters, 99L, BotRole.BATTER);

            assertThat(plan.keepExisting()).isFalse();
        }
    }

    @Nested
    @DisplayName("빈자리 계산")
    class Gaps {

        @Test
        @DisplayName("리더만 타자면 봇 8 + 선발은 생성 후 지정")
        void leaderBatterAlone() {
            FullRosterGapPlanner.Plan plan = FullRosterGapPlanner.plan(
                    1L, List.of(1L), List.of(), null, BotRole.BATTER);

            assertThat(plan.batterUserIds()).containsExactly(1L);
            assertThat(plan.pitcherUserId()).isNull();
            assertThat(plan.battersToCreate()).isEqualTo(8);
        }

        @Test
        @DisplayName("리더만 투수면 봇 8 + 선발은 리더")
        void leaderPitcherAlone() {
            FullRosterGapPlanner.Plan plan = FullRosterGapPlanner.plan(
                    1L, List.of(1L), List.of(), null, BotRole.PITCHER);

            assertThat(plan.batterUserIds()).containsExactly(1L);
            assertThat(plan.pitcherUserId()).isEqualTo(1L);
            assertThat(plan.battersToCreate()).isEqualTo(8);
        }

        @Test
        @DisplayName("멤버가 있으면 봇 생성을 줄인다")
        void useExistingMembers() {
            List<Long> members = LongStream.rangeClosed(1, 4).boxed().toList();
            FullRosterGapPlanner.Plan plan = FullRosterGapPlanner.plan(
                    1L, members, List.of(), null, BotRole.BATTER);

            assertThat(plan.batterUserIds()).containsExactly(1L, 2L, 3L, 4L);
            assertThat(plan.pitcherUserId()).isEqualTo(2L);
            assertThat(plan.battersToCreate()).isEqualTo(5);
        }
    }
}
