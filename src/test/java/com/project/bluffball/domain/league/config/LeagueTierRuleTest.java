package com.project.bluffball.domain.league.config;

import com.project.bluffball.domain.league.enums.LeagueTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LeagueTierRule")
class LeagueTierRuleTest {

    @Test
    @DisplayName("최하위는 아마4, 다음·이전 티어 사다리가 맞다")
    void ladder() {
        assertThat(LeagueTierRule.lowestTier()).isEqualTo(LeagueTier.AMATEUR_4);
        assertThat(LeagueTierRule.nextTier(LeagueTier.AMATEUR_4)).isEqualTo(LeagueTier.AMATEUR_3);
        assertThat(LeagueTierRule.previousTier(LeagueTier.AMATEUR_3)).isEqualTo(LeagueTier.AMATEUR_4);
        assertThat(LeagueTierRule.nextTier(LeagueTier.PRO_2)).isNull();
        assertThat(LeagueTierRule.previousTier(LeagueTier.AMATEUR_4)).isNull();
    }

    @Nested
    @DisplayName("아마4")
    class Amateur4 {

        private final LeagueTierRule rule = LeagueTierRule.of(LeagueTier.AMATEUR_4);

        @Test
        @DisplayName("밴드·강등 기준")
        void band() {
            assertThat(rule.floor()).isZero();
            assertThat(rule.ceil()).isEqualTo(150);
            assertThat(rule.demoteBelow()).isNull();
        }

        @Test
        @DisplayName("상한 도달 시 승급 자격")
        void promoteReady() {
            assertThat(rule.isPromoteReady(149)).isFalse();
            assertThat(rule.isPromoteReady(150)).isTrue();
        }

        @Test
        @DisplayName("강등 없음, 점수는 0~150으로 클램프")
        void clampAndDemote() {
            assertThat(rule.shouldDemote(0)).isFalse();
            assertThat(rule.clamp(200)).isEqualTo(150);
            assertThat(rule.clamp(-10)).isZero();
            assertThat(rule.clamp(80)).isEqualTo(80);
        }
    }

    @Nested
    @DisplayName("아마3")
    class Amateur3 {

        private final LeagueTierRule rule = LeagueTierRule.of(LeagueTier.AMATEUR_3);

        @Test
        @DisplayName("하한 미만이면 강등 대상, 상한은 클램프")
        void demoteAndClamp() {
            assertThat(rule.demoteBelow()).isEqualTo(150);
            assertThat(rule.shouldDemote(149)).isTrue();
            assertThat(rule.shouldDemote(150)).isFalse();
            assertThat(rule.clamp(400)).isEqualTo(300);
            assertThat(rule.clamp(100)).isEqualTo(100);
        }
    }
}
