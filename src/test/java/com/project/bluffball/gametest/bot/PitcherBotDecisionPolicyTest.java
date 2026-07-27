package com.project.bluffball.gametest.bot;

import com.project.bluffball.domain.card.enums.ChangeDirection;
import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.gametest.bot.PitcherBotThrowDecision.Intent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PitcherBotDecisionPolicy")
class PitcherBotDecisionPolicyTest {

    private final PitcherBotDecisionPolicy policy = new PitcherBotDecisionPolicy();

    private static final CardInfo FOUR_SEAM =
            new CardInfo(1L, "포심 패스트볼", 0, ChangeDirection.DOWN, Timing.EARLY);
    private static final CardInfo CUTTER =
            new CardInfo(2L, "커터", 1, ChangeDirection.SIDE, Timing.EARLY);
    private static final CardInfo SLIDER =
            new CardInfo(3L, "슬라이더", 2, ChangeDirection.SIDE, Timing.NORMAL);
    private static final CardInfo CURVE =
            new CardInfo(4L, "커브", 3, ChangeDirection.DOWN, Timing.LATE);

    @Nested
    @DisplayName("의도(카운트)")
    class IntentByCount {

        @Test
        @DisplayName("0-0·불리·동점은 CHALLENGE")
        void challengeCounts() {
            assertThat(policy.resolveIntent(0, 0)).isEqualTo(Intent.CHALLENGE);
            assertThat(policy.resolveIntent(2, 0)).isEqualTo(Intent.CHALLENGE);
            assertThat(policy.resolveIntent(1, 1)).isEqualTo(Intent.CHALLENGE);
        }

        @Test
        @DisplayName("스트라이크가 더 많으면 BAIT")
        void baitWhenAhead() {
            assertThat(policy.resolveIntent(0, 1)).isEqualTo(Intent.BAIT);
            assertThat(policy.resolveIntent(0, 2)).isEqualTo(Intent.BAIT);
            assertThat(policy.resolveIntent(1, 2)).isEqualTo(Intent.BAIT);
        }
    }

    @Nested
    @DisplayName("구종 분류")
    class Classify {

        @Test
        @DisplayName("변화량 작으면 직구형")
        void challengePitches() {
            assertThat(policy.isChallengePitch(FOUR_SEAM)).isTrue();
            assertThat(policy.isChallengePitch(CUTTER)).isTrue();
            assertThat(policy.isBaitPitch(SLIDER)).isTrue();
            assertThat(policy.isBaitPitch(CURVE)).isTrue();
        }
    }

    @Nested
    @DisplayName("decide")
    class Decide {

        @Test
        @DisplayName("초구에 직구형이 있으면 포심류 + 존 스트라이크")
        void firstPitchChallenge() {
            List<CardInfo> hand = List.of(FOUR_SEAM, SLIDER, CURVE);

            PitcherBotThrowDecision decision = policy.decide(hand, 0, 0);

            assertThat(decision.intent()).isEqualTo(Intent.CHALLENGE);
            assertThat(decision.pitchCardId()).isEqualTo(1L);
            assertThat(PitchTrajectory.isStrikeZone(
                    PitchTrajectory.finalCoordinate(decision.startCoordinateNumber(), FOUR_SEAM)))
                    .isTrue();
        }

        @Test
        @DisplayName("유리 카운트면 유인구형 우선")
        void aheadUsesBait() {
            List<CardInfo> hand = List.of(FOUR_SEAM, SLIDER, CURVE);

            PitcherBotThrowDecision decision = policy.decide(hand, 0, 2);

            assertThat(decision.intent()).isEqualTo(Intent.BAIT);
            assertThat(decision.pitchCardId()).isIn(3L, 4L);
            int fin = PitchTrajectory.finalCoordinate(
                    decision.startCoordinateNumber(),
                    decision.pitchCardId() == 3L ? SLIDER : CURVE);
            assertThat(PitchTrajectory.isStrikeZone(fin)).isFalse();
        }

        @Test
        @DisplayName("직구형 없으면 핸드에서 변화량 최소로 승부")
        void noChallengePitchFallback() {
            List<CardInfo> hand = List.of(SLIDER, CURVE);

            PitcherBotThrowDecision decision = policy.decide(hand, 0, 0);

            assertThat(decision.intent()).isEqualTo(Intent.CHALLENGE);
            assertThat(decision.pitchCardId()).isEqualTo(3L); // 변화량 2 < 3
        }

        @Test
        @DisplayName("유인구형 없으면 핸드 구종으로라도 BAIT 의도 유지")
        void noBaitPitchFallback() {
            List<CardInfo> hand = List.of(FOUR_SEAM, CUTTER);

            PitcherBotThrowDecision decision = policy.decide(hand, 0, 2);

            assertThat(decision.intent()).isEqualTo(Intent.BAIT);
            assertThat(decision.pitchCardId()).isIn(1L, 2L);
        }

        @Test
        @DisplayName("핸드 비면 예외")
        void emptyHand() {
            assertThatThrownBy(() -> policy.decide(List.of(), 0, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
