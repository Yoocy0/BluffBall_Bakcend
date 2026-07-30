package com.project.bluffball.domain.game.redis;

import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.user.record.enums.GameMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatchInfoPitcherSubstituteLineupTest {

    @Test
    @DisplayName("Compact: 전담 투수 강판 시 타자 슬롯을 이어받아 타순에 들어간다")
    void compactDedicatedPitcherEntersBattingOrder() {
        Long dedicatedPitcher = 10L;
        Long batterA = 1L;
        Long batterB = 2L;
        Long batterC = 3L;

        MatchInfo match = MatchInfo.createLeague(
                "m1",
                GameMode.COMPACT_LEAGUE,
                LeagueTier.AMATEUR_4,
                100L,
                200L,
                1L,
                4L,
                dedicatedPitcher,
                20L,
                List.of(batterA, batterB, batterC),
                List.of(4L, 5L, 6L));

        match.swapDefendingBattingOrderOnPitcherSubstitute(dedicatedPitcher, batterB);
        match.substitutePitcher(batterB);

        assertThat(match.resolveHomeBattingOrder())
                .containsExactly(batterA, dedicatedPitcher, batterC)
                .doesNotContain(batterB);
        assertThat(match.getPitcherUserId()).isEqualTo(batterB);
        assertThat(match.isBattingOrderMember(dedicatedPitcher)).isTrue();
        assertThat(match.isBattingOrderMember(batterB)).isFalse();
    }

    @Test
    @DisplayName("Full: 타순에 있던 투수·타자는 자리를 맞바꾼다")
    void fullPitcherSwapsBattingSlots() {
        Long pitcher = 9L;
        List<Long> order = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, pitcher);

        MatchInfo match = MatchInfo.createLeague(
                "m2",
                GameMode.FULL_LEAGUE,
                LeagueTier.AMATEUR_4,
                100L,
                200L,
                1L,
                11L,
                pitcher,
                19L,
                order,
                List.of(11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L));

        match.swapDefendingBattingOrderOnPitcherSubstitute(pitcher, 5L);
        match.substitutePitcher(5L);

        assertThat(match.resolveHomeBattingOrder().get(4)).isEqualTo(pitcher);
        assertThat(match.resolveHomeBattingOrder().get(8)).isEqualTo(5L);
        assertThat(match.getPitcherUserId()).isEqualTo(5L);
    }
}
