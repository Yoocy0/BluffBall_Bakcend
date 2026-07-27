package com.project.bluffball.domain.league.entity;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TeamLeagueProgress")
class TeamLeagueProgressTest {

    @Test
    @DisplayName("생성 시 아마4·rating 0")
    void create() {
        TeamLeagueProgress progress = new TeamLeagueProgress(1L, LeagueFormat.COMPACT);

        assertThat(progress.getTeamId()).isEqualTo(1L);
        assertThat(progress.getFormat()).isEqualTo(LeagueFormat.COMPACT);
        assertThat(progress.getCurrentTier()).isEqualTo(LeagueTier.AMATEUR_4);
        assertThat(progress.getRating()).isZero();
        assertThat(progress.getWins()).isZero();
        assertThat(progress.getLoses()).isZero();
        assertThat(progress.isPromoteReady()).isFalse();
        assertThat(progress.shouldDemote()).isFalse();
    }

    @Nested
    @DisplayName("applyMatchResult")
    class ApplyMatchResult {

        @Test
        @DisplayName("승리 시 점수·전적이 오른다")
        void win() {
            TeamLeagueProgress progress = new TeamLeagueProgress(1L, LeagueFormat.COMPACT);

            progress.applyMatchResult(true, 3);

            assertThat(progress.getWins()).isEqualTo(1);
            assertThat(progress.getRating()).isEqualTo(8); // 5 + 3
            assertThat(progress.getRunDiff()).isEqualTo(3);
        }

        @Test
        @DisplayName("패배 시 점수·전적이 반영된다")
        void loss() {
            TeamLeagueProgress progress = new TeamLeagueProgress(1L, LeagueFormat.COMPACT);
            progress.applyMatchResult(true, 10); // rating 15

            progress.applyMatchResult(false, -2);

            assertThat(progress.getLoses()).isEqualTo(1);
            assertThat(progress.getRating()).isEqualTo(8); // 15 + (-2) - 5
            assertThat(progress.getRunDiff()).isEqualTo(8);
        }

        @Test
        @DisplayName("상한 도달 후 승리해도 rating은 +0, 전적만 오른다")
        void winAtCeilGivesZeroRating() {
            TeamLeagueProgress progress = new TeamLeagueProgress(1L, LeagueFormat.COMPACT);
            progress.applyMatchResult(true, 200); // clamp 150, promote ready

            progress.applyMatchResult(true, 10);

            assertThat(progress.getRating()).isEqualTo(150);
            assertThat(progress.getWins()).isEqualTo(2);
            assertThat(progress.getRunDiff()).isEqualTo(210);
            assertThat(progress.isPromoteReady()).isTrue();
        }

        @Test
        @DisplayName("상한에서도 패배하면 점수가 내려간다")
        void lossAtCeilDecreases() {
            TeamLeagueProgress progress = new TeamLeagueProgress(1L, LeagueFormat.COMPACT);
            progress.applyMatchResult(true, 200);

            progress.applyMatchResult(false, -2);

            assertThat(progress.getRating()).isEqualTo(143); // 150 - 2 - 5
            assertThat(progress.isPromoteReady()).isFalse();
        }
    }

    @Nested
    @DisplayName("주기 감쇠·승급·강등")
    class DecayPromoteDemote {

        @Test
        @DisplayName("승급 시 상위 티어 floor로 맞춘다")
        void promote() {
            TeamLeagueProgress progress = new TeamLeagueProgress(1L, LeagueFormat.COMPACT);
            progress.applyMatchResult(true, 200);

            progress.promoteTo(LeagueTier.AMATEUR_3);

            assertThat(progress.getCurrentTier()).isEqualTo(LeagueTier.AMATEUR_3);
            assertThat(progress.getRating()).isEqualTo(150);
            assertThat(progress.isPromoteReady()).isFalse();
        }

        @Test
        @DisplayName("주기 -30 후 demoteBelow 미만이면 강등 대상")
        void periodicDecayThenDemote() {
            TeamLeagueProgress progress = new TeamLeagueProgress(1L, LeagueFormat.COMPACT);
            progress.applyMatchResult(true, 200);
            progress.promoteTo(LeagueTier.AMATEUR_3); // rating 150

            progress.applyPeriodicDecay(); // 120

            assertThat(progress.getRating()).isEqualTo(120);
            assertThat(progress.shouldDemote()).isTrue();

            progress.demoteTo(LeagueTier.AMATEUR_4);

            assertThat(progress.getCurrentTier()).isEqualTo(LeagueTier.AMATEUR_4);
            assertThat(progress.getRating()).isEqualTo(150);
            assertThat(progress.shouldDemote()).isFalse();
            assertThat(progress.isPromoteReady()).isTrue(); // 다시 자격만 — 참가비는 promote에서
        }

        @Test
        @DisplayName("감쇠 후에도 demoteBelow 이상이면 잔류")
        void decayButStay() {
            TeamLeagueProgress progress = new TeamLeagueProgress(1L, LeagueFormat.COMPACT);
            progress.applyMatchResult(true, 200);
            progress.promoteTo(LeagueTier.AMATEUR_3);
            progress.applyMatchResult(true, 20); // 175
            progress.applyMatchResult(true, 20); // 200
            progress.applyMatchResult(true, 20); // 225
            progress.applyMatchResult(true, 20); // 250

            progress.applyPeriodicDecay(); // 220

            assertThat(progress.getRating()).isEqualTo(220);
            assertThat(progress.shouldDemote()).isFalse();
        }

        @Test
        @DisplayName("아마4는 감쇠만 하고 강등하지 않는다")
        void amateur4DecayOnly() {
            TeamLeagueProgress progress = new TeamLeagueProgress(1L, LeagueFormat.COMPACT);
            progress.applyMatchResult(true, 20); // 25

            progress.applyPeriodicDecay(); // 0 (clamp)

            assertThat(progress.getRating()).isZero();
            assertThat(progress.shouldDemote()).isFalse();
        }
    }
}
