package com.project.bluffball.domain.game.bot;

import com.project.bluffball.domain.card.enums.ChangeDirection;
import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.enums.BotDifficulty;
import com.project.bluffball.domain.game.enums.Timing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BatterBotDecisionPolicy")
class BatterBotDecisionPolicyTest {

    private final PitcherBotDecisionPolicy pitcherPolicy = new PitcherBotDecisionPolicy();
    private final BatterBotDecisionPolicy policy =
            new BatterBotDecisionPolicy(pitcherPolicy, new Random(1L));

    private static final CardInfo FOUR_SEAM =
            new CardInfo(1L, "포심 패스트볼", 0, ChangeDirection.DOWN, Timing.EARLY);
    private static final CardInfo TWO_SEAM =
            new CardInfo(2L, "투심 패스트볼", 1, ChangeDirection.SIDE, Timing.EARLY);
    private static final CardInfo SLIDER =
            new CardInfo(4L, "슬라이더", 2, ChangeDirection.SIDE, Timing.NORMAL);
    private static final CardInfo CURVE =
            new CardInfo(5L, "커브", 3, ChangeDirection.DOWN, Timing.LATE);
    private static final CardInfo CHANGEUP =
            new CardInfo(6L, "체인지업", 2, ChangeDirection.DOWN, Timing.LATE);

    /** 시작 13에서 SIDE+2 → 최종 15(존 밖) */
    private static final CardInfo SIDE_OUT =
            new CardInfo(10L, "사이드아웃", 2, ChangeDirection.SIDE, Timing.NORMAL);

    /** 시작 13에서 DOWN+1 → 최종 18(존 안) */
    private static final CardInfo DOWN_IN =
            new CardInfo(11L, "다운인", 1, ChangeDirection.DOWN, Timing.NORMAL);

    @Nested
    @DisplayName("카운트 뒤집기")
    class CountFlip {

        @Test
        @DisplayName("strikes ≤ balls 이면 타자 유리")
        void batterFavorableWhenStrikesNotGreater() {
            assertThat(BatterBotDecisionPolicy.isBatterFavorable(0, 0)).isTrue();
            assertThat(BatterBotDecisionPolicy.isBatterFavorable(2, 1)).isTrue();
            assertThat(BatterBotDecisionPolicy.isBatterFavorable(1, 1)).isTrue();
        }

        @Test
        @DisplayName("strikes > balls 이면 타자 불리")
        void batterUnfavorableWhenPitcherAhead() {
            assertThat(BatterBotDecisionPolicy.isBatterFavorable(0, 2)).isFalse();
            assertThat(BatterBotDecisionPolicy.isBatterFavorable(1, 2)).isFalse();
        }
    }

    @Nested
    @DisplayName("EASY")
    class Easy {

        @Test
        @DisplayName("비소극이면 항상 스윙(좌표 1~25)")
        void alwaysSwingsWhenNotPassive() {
            List<CardInfo> pool = List.of(FOUR_SEAM, SLIDER, CURVE);

            BatterBotSwingDecision favorable =
                    policy.decide(13, 2, 0, BotDifficulty.EASY, pool, false);
            BatterBotSwingDecision unfavorable =
                    policy.decide(13, 0, 2, BotDifficulty.EASY, pool, false);

            assertThat(favorable.swing()).isTrue();
            assertThat(favorable.batterCoordinateNumber()).isBetween(1, 25);
            assertThat(unfavorable.swing()).isTrue();
            assertThat(unfavorable.batterCoordinateNumber()).isBetween(1, 25);
        }

        @Test
        @DisplayName("소극 모드: 선호 유형에 존·볼 모두 있으면 지켜보기")
        void passiveTakesWhenBothOutcomes() {
            // 타자 불리 → 변화구; DOWN_IN(존) + SIDE_OUT(볼) — passiveMode = seen < 2
            List<CardInfo> pool = List.of(FOUR_SEAM, DOWN_IN, SIDE_OUT);

            BatterBotSwingDecision decision =
                    policy.decide(13, 0, 2, BotDifficulty.EASY, pool, true);

            assertThat(decision.swing()).isFalse();
            assertThat(decision.batterCoordinateNumber()).isZero();
        }

        @Test
        @DisplayName("소극 모드라도 한쪽만 있으면 일반 규칙(스윙)")
        void passiveSwingsWhenUniform() {
            List<CardInfo> pool = List.of(FOUR_SEAM, DOWN_IN);

            BatterBotSwingDecision decision =
                    policy.decide(13, 0, 2, BotDifficulty.EASY, pool, true);

            assertThat(decision.swing()).isTrue();
            assertThat(decision.batterCoordinateNumber())
                    .isEqualTo(PitchTrajectory.finalCoordinate(13, DOWN_IN));
        }

        @Test
        @DisplayName("타자 유리면 패스트볼 노림")
        void favorableAnticipatesFastball() {
            List<CardInfo> pool = List.of(FOUR_SEAM, SLIDER, CURVE);

            BatterBotSwingDecision decision =
                    policy.decide(13, 2, 0, BotDifficulty.EASY, pool, false);

            assertThat(decision.assumedPitchName()).contains("패스트볼");
            assertThat(decision.batterCoordinateNumber())
                    .isEqualTo(PitchTrajectory.finalCoordinate(13, FOUR_SEAM));
            assertThat(decision.timing()).isEqualTo(Timing.EARLY);
        }

        @Test
        @DisplayName("타자 불리면 존 안 변화구만 후보")
        void unfavorablePicksInZoneBreaking() {
            // start=13: DOWN_IN → 18 존, SIDE_OUT → 15 밖, CURVE DOWN3 → 격자이탈 0
            List<CardInfo> pool = List.of(FOUR_SEAM, DOWN_IN, SIDE_OUT, CURVE);
            BatterBotDecisionPolicy seeded =
                    new BatterBotDecisionPolicy(pitcherPolicy, new Random(42L));

            for (int i = 0; i < 20; i++) {
                BatterBotSwingDecision decision =
                        seeded.decide(13, 0, 2, BotDifficulty.EASY, pool, false);
                assertThat(decision.swing()).isTrue();
                assertThat(decision.assumedPitchName()).isEqualTo(DOWN_IN.name());
                assertThat(PitchTrajectory.isStrikeZone(decision.batterCoordinateNumber())).isTrue();
            }
        }

        @Test
        @DisplayName("패스트볼 없으면 NORMAL 대체 순위")
        void missingFastballUsesNormalSubstitute() {
            List<CardInfo> pool = List.of(CHANGEUP, SLIDER, CURVE);

            BatterBotSwingDecision decision =
                    policy.decide(13, 0, 0, BotDifficulty.EASY, pool, false);

            // NORMAL substitute: 최소 변화량 → SLIDER(2 SIDE) vs CHANGEUP(2 DOWN) → DOWN 우선
            assertThat(decision.assumedPitchName()).isEqualTo(CHANGEUP.name());
            assertThat(decision.swing()).isTrue();
        }
    }

    @Nested
    @DisplayName("NORMAL")
    class Normal {

        @Test
        @DisplayName("예상 최종이 존이면 스윙")
        void swingsWhenAnticipatedStrike() {
            List<CardInfo> pool = List.of(FOUR_SEAM, SLIDER);

            // 타자 유리 → 패스트볼, start 13 → 최종 13 존
            BatterBotSwingDecision decision =
                    policy.decide(13, 0, 0, BotDifficulty.NORMAL, pool, false);

            assertThat(decision.swing()).isTrue();
            assertThat(decision.batterCoordinateNumber()).isEqualTo(13);
            assertThat(decision.assumedPitchName()).contains("패스트볼");
        }

        @Test
        @DisplayName("예상 최종이 볼이면 좌표 0")
        void takesWhenAnticipatedBall() {
            // 타자 불리 → 변화구만; SIDE_OUT만 있으면 최종 15 볼
            List<CardInfo> pool = List.of(FOUR_SEAM, SIDE_OUT);

            BatterBotSwingDecision decision =
                    policy.decide(13, 0, 2, BotDifficulty.NORMAL, pool, false);

            assertThat(decision.swing()).isFalse();
            assertThat(decision.batterCoordinateNumber()).isZero();
            assertThat(decision.assumedPitchName()).isEqualTo(SIDE_OUT.name());
        }

        @Test
        @DisplayName("소극 모드(seen < 2): 선호 유형에 존·볼 모두 있으면 지켜보기")
        void passiveTakesWhenBothOutcomes() {
            // 타자 불리 → 변화구; DOWN_IN(존) + SIDE_OUT(볼) — passiveMode = seen < 2
            List<CardInfo> pool = List.of(FOUR_SEAM, DOWN_IN, SIDE_OUT);

            BatterBotSwingDecision decision =
                    policy.decide(13, 0, 2, BotDifficulty.NORMAL, pool, true);

            assertThat(decision.swing()).isFalse();
            assertThat(decision.batterCoordinateNumber()).isZero();
        }

        @Test
        @DisplayName("소극 모드라도 한쪽만 있으면 일반 규칙")
        void passiveSameRulesWhenUniform() {
            // passiveMode=true여도 결과가 균일하면 NORMAL 일반 규칙
            List<CardInfo> pool = List.of(FOUR_SEAM, DOWN_IN);

            BatterBotSwingDecision decision =
                    policy.decide(13, 0, 2, BotDifficulty.NORMAL, pool, true);

            assertThat(decision.swing()).isTrue();
            assertThat(decision.batterCoordinateNumber())
                    .isEqualTo(PitchTrajectory.finalCoordinate(13, DOWN_IN));
        }
    }

    @Nested
    @DisplayName("HARD")
    class Hard {

        @Test
        @DisplayName("투수 유리 카운트면 FAVORABLE 우선순위 표 사용")
        void usesPitcherFavorableTable() {
            List<CardInfo> pool = List.of(FOUR_SEAM, DOWN_IN, SIDE_OUT);
            // strikes > balls → pitcher favorable; 타자는 불리 → 변화구 양존 시 STRIKE만
            BatterBotDecisionPolicy seeded =
                    new BatterBotDecisionPolicy(pitcherPolicy, new Random(7L));

            List<PitcherBotHardOption> options =
                    seeded.planHardBatterOptions(13, pool, 0, 2, false);

            assertThat(options).isNotEmpty();
            // pitcher FAVORABLE 첫 후보 BREAKING_BALL — 타자 불리+양존이면 BALL 필터로 스킵될 수 있음
            assertThat(options.stream().map(PitcherBotHardOption::kind).toList())
                    .isSubsetOf(
                            PitcherBotDecisionPolicy.HARD_FAVORABLE_PRIORITY);
            assertThat(options.get(0).weight()).isEqualTo(40);
        }

        @Test
        @DisplayName("변화구 양존 시 타자 유리면 BALL만 실현")
        void breakingBothZonesFavorableForcesBall() {
            List<CardInfo> pool = List.of(FOUR_SEAM, DOWN_IN, SIDE_OUT);
            BatterBotDecisionPolicy seeded =
                    new BatterBotDecisionPolicy(pitcherPolicy, new Random(3L));

            List<PitcherBotHardOption> options =
                    seeded.planHardBatterOptions(13, pool, 2, 0, true);

            List<PitcherBotOptionKind> kinds =
                    options.stream().map(PitcherBotHardOption::kind).toList();
            assertThat(kinds).doesNotContain(PitcherBotOptionKind.BREAKING_STRIKE);
            assertThat(kinds).contains(PitcherBotOptionKind.BREAKING_BALL);
        }

        @Test
        @DisplayName("BALL 의도인데 최종이 존이면 지켜보기")
        void ballIntentTakesIfFinalInZone() {
            // 패스트볼만 있고 시작 13 → 최종 존 — FASTBALL_BALL 실현 불가,
            // 변화구 없이 STRIKE 옵션만 → 스윙. 대신 강제 매핑 단위 검증은 plan으로.
            List<CardInfo> pool = List.of(FOUR_SEAM);
            BatterBotSwingDecision decision =
                    policy.decide(13, 0, 0, BotDifficulty.HARD, pool, false);

            assertThat(decision.swing()).isTrue();
            assertThat(decision.batterCoordinateNumber()).isEqualTo(13);
        }

        @Test
        @DisplayName("소극 모드(seen < 2): 풀에 존·볼 모두 있으면 지켜보기")
        void passiveTakesWhenUncertain() {
            // passiveMode = seen < 2; HARD는 전체 풀 기준
            List<CardInfo> pool = List.of(FOUR_SEAM, SIDE_OUT);

            BatterBotSwingDecision decision =
                    policy.decide(13, 0, 0, BotDifficulty.HARD, pool, true);

            assertThat(decision.swing()).isFalse();
            assertThat(decision.batterCoordinateNumber()).isZero();
        }
    }

    @Nested
    @DisplayName("검증")
    class Validation {

        @Test
        @DisplayName("빈 풀·null 난이도는 예외")
        void rejectsEmptyPoolAndNullDifficulty() {
            assertThatThrownBy(() ->
                    policy.decide(13, 0, 0, BotDifficulty.EASY, List.of(), false))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() ->
                    policy.decide(13, 0, 0, null, List.of(FOUR_SEAM), false))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("BatterBotPitchMemory")
    class Memory {

        @Test
        @DisplayName("maxKnown 미만이면 전체 카탈로그")
        void fullCatalogUntilHandSizeSeen() {
            BatterBotPitchMemory memory = new BatterBotPitchMemory();
            memory.remember(1L);
            memory.remember(4L);
            List<CardInfo> catalog = List.of(FOUR_SEAM, TWO_SEAM, SLIDER, CURVE);

            List<CardInfo> pool = memory.considerationPool(catalog, 3);

            assertThat(pool).hasSize(4);
        }

        @Test
        @DisplayName("maxKnown 이상이면 확인 구종만")
        void restrictsToSeenWhenFull() {
            BatterBotPitchMemory memory = new BatterBotPitchMemory();
            memory.remember(1L);
            memory.remember(4L);
            memory.remember(5L);
            List<CardInfo> catalog = List.of(FOUR_SEAM, TWO_SEAM, SLIDER, CURVE);

            List<CardInfo> pool = memory.considerationPool(catalog, 3);

            assertThat(pool).extracting(CardInfo::cardId).containsExactly(1L, 4L, 5L);
        }

        @Test
        @DisplayName("중복 remember는 크기 증가 없음")
        void rememberIsDistinct() {
            BatterBotPitchMemory memory = new BatterBotPitchMemory();
            memory.remember(1L);
            memory.remember(1L);

            assertThat(memory.size()).isEqualTo(1);
            assertThat(memory.isPassive()).isTrue();
        }

        @Test
        @DisplayName("서로 다른 구종 2개 미만이면 소극")
        void isPassiveUntilTwoDistinctSeen() {
            BatterBotPitchMemory memory = new BatterBotPitchMemory();
            assertThat(memory.isPassive()).isTrue();

            memory.remember(1L);
            assertThat(memory.isPassive()).isTrue();
            assertThat(memory.size()).isLessThan(BatterBotPitchMemory.PASSIVE_UNTIL_DISTINCT_SEEN);

            memory.remember(4L);
            assertThat(memory.isPassive()).isFalse();
            assertThat(memory.size()).isGreaterThanOrEqualTo(
                    BatterBotPitchMemory.PASSIVE_UNTIL_DISTINCT_SEEN);
        }
    }
}
