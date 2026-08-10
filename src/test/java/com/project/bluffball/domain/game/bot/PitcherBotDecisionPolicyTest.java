package com.project.bluffball.domain.game.bot;

import com.project.bluffball.domain.card.enums.ChangeDirection;
import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.enums.BotDifficulty;
import com.project.bluffball.domain.game.enums.Timing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("PitcherBotDecisionPolicy")
class PitcherBotDecisionPolicyTest {

    private final PitcherBotDecisionPolicy policy = new PitcherBotDecisionPolicy();

    private static final CardInfo FOUR_SEAM =
            new CardInfo(1L, "포심 패스트볼", 0, ChangeDirection.DOWN, Timing.EARLY);
    private static final CardInfo TWO_SEAM =
            new CardInfo(2L, "투심 패스트볼", 1, ChangeDirection.SIDE, Timing.EARLY);
    private static final CardInfo CUTTER =
            new CardInfo(3L, "커터", 1, ChangeDirection.SIDE, Timing.EARLY);
    private static final CardInfo SLIDER =
            new CardInfo(4L, "슬라이더", 2, ChangeDirection.SIDE, Timing.NORMAL);
    private static final CardInfo CURVE =
            new CardInfo(5L, "커브", 3, ChangeDirection.DOWN, Timing.LATE);
    private static final CardInfo CHANGEUP =
            new CardInfo(6L, "체인지업", 2, ChangeDirection.DOWN, Timing.LATE);
    private static final CardInfo SMALL_REVERSE =
            new CardInfo(7L, "작은역회전", 1, ChangeDirection.REVERSE, Timing.NORMAL);
    private static final CardInfo SMALL_SIDE =
            new CardInfo(8L, "작은횡변화", 1, ChangeDirection.SIDE, Timing.NORMAL);

    /** 어떤 시작에서도 최종이 존 밖(격자 이탈) — STRIKE 옵션 실현 불가용 */
    private static final CardInfo EXTREME_DOWN =
            new CardInfo(99L, "극단낙하", 4, ChangeDirection.DOWN, Timing.LATE);

    @Nested
    @DisplayName("구종 선택")
    class PitchSelection {

        @Test
        @DisplayName("유리 카운트면 변화구(비패스트볼) — 양쪽 타입 있을 때")
        void favorablePicksBreaking() {
            List<CardInfo> hand = List.of(FOUR_SEAM, SLIDER, CURVE);

            PitcherBotThrowDecision easy = policy.decide(hand, 0, 2, BotDifficulty.EASY);
            PitcherBotThrowDecision normal = policy.decide(hand, 1, 2, BotDifficulty.NORMAL);

            assertThat(easy.pitchCardId()).isEqualTo(CURVE.cardId());
            assertThat(normal.pitchCardId()).isEqualTo(CURVE.cardId());
        }

        @Test
        @DisplayName("불리·동점 카운트면 패스트볼")
        void unfavorablePicksFastball() {
            List<CardInfo> hand = List.of(FOUR_SEAM, SLIDER, CURVE);

            PitcherBotThrowDecision zeroZero = policy.decide(hand, 0, 0, BotDifficulty.EASY);
            PitcherBotThrowDecision behind = policy.decide(hand, 2, 0, BotDifficulty.NORMAL);
            PitcherBotThrowDecision tied = policy.decide(hand, 1, 1, BotDifficulty.EASY);

            assertThat(zeroZero.pitchCardId()).isEqualTo(FOUR_SEAM.cardId());
            assertThat(behind.pitchCardId()).isEqualTo(FOUR_SEAM.cardId());
            assertThat(tied.pitchCardId()).isEqualTo(FOUR_SEAM.cardId());
        }

        @Test
        @DisplayName("이름에 패스트볼이 있으면 changeAmount>0이어도 패스트볼")
        void nameBasedFastball() {
            List<CardInfo> hand = List.of(TWO_SEAM, SLIDER);

            PitcherBotThrowDecision decision = policy.decide(hand, 0, 0, BotDifficulty.NORMAL);

            assertThat(decision.pitchCardId()).isEqualTo(TWO_SEAM.cardId());
        }
    }

    @Nested
    @DisplayName("패스트볼 대체 순위")
    class FastballSubstitute {

        @Test
        @DisplayName("EASY: DOWN 중 최소 변화량 우선")
        void easyPrefersDownSmallest() {
            // DOWN(3), DOWN(2), SIDE(1) — EASY는 DOWN 중 최소(2)
            List<CardInfo> hand = List.of(CURVE, CHANGEUP, CUTTER);

            CardInfo picked = policy.substituteMissingFastball(hand, BotDifficulty.EASY);

            assertThat(picked.cardId()).isEqualTo(CHANGEUP.cardId());
        }

        @Test
        @DisplayName("EASY: DOWN 없으면 최소 변화량 후 DOWN>SIDE>REVERSE")
        void easyNoDownFallsBack() {
            List<CardInfo> hand = List.of(SLIDER, SMALL_REVERSE, SMALL_SIDE);

            CardInfo picked = policy.substituteMissingFastball(hand, BotDifficulty.EASY);

            // changeAmount 1 중 SIDE(rank1) < REVERSE(rank2)
            assertThat(picked.cardId()).isEqualTo(SMALL_SIDE.cardId());
        }

        @Test
        @DisplayName("NORMAL: 최소 변화량 후 DOWN>SIDE>REVERSE")
        void normalSmallestThenDirection() {
            // changeAmount 1: SIDE vs REVERSE → SIDE 우선
            List<CardInfo> hand = List.of(CHANGEUP, SMALL_REVERSE, SMALL_SIDE);

            CardInfo picked = policy.substituteMissingFastball(hand, BotDifficulty.NORMAL);

            assertThat(picked.cardId()).isEqualTo(SMALL_SIDE.cardId());
        }

        @Test
        @DisplayName("NORMAL: 동점 변화량이면 DOWN 우선 — EASY와 다르게 DOWN이 커도 작은 변화량 선택")
        void normalVsEasyWhenDownIsLarger() {
            // DOWN(2), SIDE(1) — EASY picks DOWN(2), NORMAL picks SIDE(1)
            List<CardInfo> hand = List.of(CHANGEUP, CUTTER);

            CardInfo easy = policy.substituteMissingFastball(hand, BotDifficulty.EASY);
            CardInfo normal = policy.substituteMissingFastball(hand, BotDifficulty.NORMAL);

            assertThat(easy.cardId()).isEqualTo(CHANGEUP.cardId());
            assertThat(normal.cardId()).isEqualTo(CUTTER.cardId());
        }
    }

    @Nested
    @DisplayName("위치(볼/스트라이크)")
    class Location {

        @Test
        @DisplayName("EASY: 선택 구종의 최종 좌표가 항상 존 안")
        void easyAlwaysInZone() {
            List<CardInfo> hand = List.of(FOUR_SEAM, SLIDER, CURVE);

            for (int balls = 0; balls <= 3; balls++) {
                for (int strikes = 0; strikes <= 2; strikes++) {
                    PitcherBotThrowDecision decision =
                            policy.decide(hand, balls, strikes, BotDifficulty.EASY);
                    CardInfo pitch = byId(hand).get(decision.pitchCardId());
                    int fin = PitchTrajectory.finalCoordinate(
                            decision.startCoordinateNumber(), pitch);
                    assertThat(PitchTrajectory.isStrikeZone(fin))
                            .as("count %d-%d pitch=%s start=%d fin=%d",
                                    balls, strikes, pitch.name(),
                                    decision.startCoordinateNumber(), fin)
                            .isTrue();
                }
            }
        }

        @Test
        @DisplayName("NORMAL 유리: 최종 좌표가 존 밖(유인구) 가능")
        void normalFavorableCanBeOutOfZone() {
            List<CardInfo> hand = List.of(FOUR_SEAM, SLIDER, CURVE);

            PitcherBotThrowDecision decision = policy.decide(hand, 0, 2, BotDifficulty.NORMAL);

            assertThat(decision.pitchCardId()).isEqualTo(CURVE.cardId());
            int fin = PitchTrajectory.finalCoordinate(decision.startCoordinateNumber(), CURVE);
            assertThat(PitchTrajectory.isStrikeZone(fin)).isFalse();
        }

        @Test
        @DisplayName("NORMAL 불리: 최종 좌표가 존 안")
        void normalUnfavorableInZone() {
            List<CardInfo> hand = List.of(FOUR_SEAM, SLIDER, CURVE);

            PitcherBotThrowDecision decision = policy.decide(hand, 0, 0, BotDifficulty.NORMAL);

            assertThat(decision.pitchCardId()).isEqualTo(FOUR_SEAM.cardId());
            int fin = PitchTrajectory.finalCoordinate(decision.startCoordinateNumber(), FOUR_SEAM);
            assertThat(PitchTrajectory.isStrikeZone(fin)).isTrue();
        }
    }

    @Nested
    @DisplayName("HARD")
    class Hard {

        @Test
        @DisplayName("유리: 옵션 종류 우선순위 BREAKING_BALL>FASTBALL_STRIKE>FASTBALL_BALL>BREAKING_STRIKE")
        void favorablePriorityOrder() {
            List<CardInfo> hand = List.of(FOUR_SEAM, CURVE);

            List<PitcherBotHardOption> options = policy.planHardOptions(hand, 0, 2);

            assertThat(options).extracting(PitcherBotHardOption::kind).containsExactly(
                    PitcherBotOptionKind.BREAKING_BALL,
                    PitcherBotOptionKind.FASTBALL_STRIKE,
                    PitcherBotOptionKind.FASTBALL_BALL,
                    PitcherBotOptionKind.BREAKING_STRIKE);
        }

        @Test
        @DisplayName("불리: 옵션 종류 우선순위 FASTBALL_STRIKE>BREAKING_STRIKE>BREAKING_BALL>FASTBALL_BALL")
        void unfavorablePriorityOrder() {
            List<CardInfo> hand = List.of(FOUR_SEAM, CURVE);

            List<PitcherBotHardOption> options = policy.planHardOptions(hand, 0, 0);

            assertThat(options).extracting(PitcherBotHardOption::kind).containsExactly(
                    PitcherBotOptionKind.FASTBALL_STRIKE,
                    PitcherBotOptionKind.BREAKING_STRIKE,
                    PitcherBotOptionKind.BREAKING_BALL,
                    PitcherBotOptionKind.FASTBALL_BALL);
        }

        @Test
        @DisplayName("4종 모두 실현 가능하면 가중치 40/30/20/10")
        void weightsWhenAllFourRealizable() {
            List<CardInfo> hand = List.of(FOUR_SEAM, CURVE);

            List<PitcherBotHardOption> favorable = policy.planHardOptions(hand, 0, 2);
            List<PitcherBotHardOption> unfavorable = policy.planHardOptions(hand, 2, 0);

            assertThat(favorable).extracting(PitcherBotHardOption::weight)
                    .containsExactly(40, 30, 20, 10);
            assertThat(unfavorable).extracting(PitcherBotHardOption::weight)
                    .containsExactly(40, 30, 20, 10);
        }

        @Test
        @DisplayName("실현 불가 종류는 건너뛰고 남은 옵션에 접두 가중치 부여")
        void unrealizableSkippedGetsPrefixWeights() {
            // STRIKE 불가 · BALL만 가능 → 유리 우선순위에서 BALL 2종만 남음
            List<CardInfo> hand = List.of(EXTREME_DOWN);

            List<PitcherBotHardOption> options = policy.planHardOptions(hand, 0, 2);

            assertThat(options).extracting(PitcherBotHardOption::kind).containsExactly(
                    PitcherBotOptionKind.BREAKING_BALL,
                    PitcherBotOptionKind.FASTBALL_BALL);
            assertThat(options).extracting(PitcherBotHardOption::weight)
                    .containsExactly(40, 30);
        }

        @Test
        @DisplayName("시드 Random으로 누적 가중 샘플이 기대 옵션을 고른다")
        void deterministicSampleWithSeededRandom() {
            List<CardInfo> hand = List.of(FOUR_SEAM, CURVE);
            List<PitcherBotHardOption> options = policy.planHardOptions(hand, 0, 2);
            assertThat(options).hasSize(4);

            // roll 45 ∈ [40,70) → 2번째(FASTBALL_STRIKE)
            Random random = mock(Random.class);
            when(random.nextInt(100)).thenReturn(45);
            PitcherBotDecisionPolicy seeded = new PitcherBotDecisionPolicy(random);

            PitcherBotHardOption picked = seeded.sampleHardOption(options);

            assertThat(picked.kind()).isEqualTo(PitcherBotOptionKind.FASTBALL_STRIKE);
            assertThat(PitcherBotDecisionPolicy.pickByCumulativeWeight(options, 0).kind())
                    .isEqualTo(PitcherBotOptionKind.BREAKING_BALL);
            assertThat(PitcherBotDecisionPolicy.pickByCumulativeWeight(options, 39).kind())
                    .isEqualTo(PitcherBotOptionKind.BREAKING_BALL);
            assertThat(PitcherBotDecisionPolicy.pickByCumulativeWeight(options, 40).kind())
                    .isEqualTo(PitcherBotOptionKind.FASTBALL_STRIKE);
            assertThat(PitcherBotDecisionPolicy.pickByCumulativeWeight(options, 99).kind())
                    .isEqualTo(PitcherBotOptionKind.BREAKING_STRIKE);
        }

        @Test
        @DisplayName("decide(HARD)는 정책 경로로 유효한 투구를 반환한다")
        void decideHardReturnsValidThrow() {
            List<CardInfo> hand = List.of(FOUR_SEAM, CURVE);

            PitcherBotThrowDecision decision = policy.decide(hand, 1, 2, BotDifficulty.HARD);

            assertThat(decision.pitchCardId()).isIn(FOUR_SEAM.cardId(), CURVE.cardId());
            assertThat(decision.startCoordinateNumber()).isBetween(1, 25);
        }
    }

    @Nested
    @DisplayName("예외")
    class Errors {

        @Test
        @DisplayName("빈 핸드·null 난이도는 예외")
        void rejectsEmptyAndNullDifficulty() {
            assertThatThrownBy(() -> policy.decide(List.of(), 0, 0, BotDifficulty.EASY))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() ->
                    policy.decide(List.of(FOUR_SEAM), 0, 0, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private static Map<Long, CardInfo> byId(List<CardInfo> hand) {
        return hand.stream().collect(Collectors.toMap(CardInfo::cardId, Function.identity()));
    }
}
