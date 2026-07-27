package com.project.bluffball.gametest.bot;

import com.project.bluffball.domain.card.enums.ChangeDirection;
import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.game.enums.TurnResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BatterBotDecisionPolicy")
class BatterBotDecisionPolicyTest {

    private final BatterBotDecisionPolicy policy = new BatterBotDecisionPolicy();

    private static final CardInfo FOUR_SEAM =
            new CardInfo(1L, "포심 패스트볼", 0, ChangeDirection.DOWN, Timing.EARLY);
    private static final CardInfo CURVE =
            new CardInfo(2L, "커브", 3, ChangeDirection.DOWN, Timing.LATE);
    private static final CardInfo SLIDER =
            new CardInfo(3L, "슬라이더", 2, ChangeDirection.SIDE, Timing.NORMAL);

    private static final List<CardInfo> CATALOG = List.of(FOUR_SEAM, CURVE, SLIDER);

    @Nested
    @DisplayName("기억·제외")
    class Memory {

        @Test
        @DisplayName("스트라이크가 아니면 후보에서 뺀다")
        void eliminateNonStrike() {
            BatterBotPitchMemory memory = new BatterBotPitchMemory();
            memory.remember("커브", TurnResult.BALL);
            memory.remember("포크", TurnResult.SINGLE);

            assertThat(memory.isEliminated("커브")).isTrue();
            assertThat(memory.isEliminated("포크")).isTrue();
            assertThat(memory.isEliminated("포심 패스트볼")).isFalse();
        }

        @Test
        @DisplayName("스트라이크·삼진 구종은 유지한다")
        void keepStrike() {
            BatterBotPitchMemory memory = new BatterBotPitchMemory();
            memory.remember("포심 패스트볼", TurnResult.STRIKE);
            memory.remember("슬라이더", TurnResult.STRIKE_OUT);

            assertThat(memory.isEliminated("포심 패스트볼")).isFalse();
            assertThat(memory.isEliminated("슬라이더")).isFalse();
        }
    }

    @Nested
    @DisplayName("스윙 판단")
    class Decide {

        @Test
        @DisplayName("중앙 시작 + 포심이면 스윙(최종=시작, EARLY)")
        void swingFourSeamAtCenter() {
            BatterBotSwingDecision decision =
                    policy.decide(13, CATALOG, new BatterBotPitchMemory());

            assertThat(decision.swing()).isTrue();
            assertThat(decision.batterCoordinateNumber()).isEqualTo(13);
            assertThat(decision.timing()).isEqualTo(Timing.EARLY);
            assertThat(decision.assumedPitchName()).isEqualTo("포심 패스트볼");
            assertThat(decision.strikeProbability()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("존에 안 들어오는 구종만 남으면 지켜보기")
        void takeWhenNoStrikeCandidate() {
            BatterBotPitchMemory memory = new BatterBotPitchMemory();
            memory.remember("포심 패스트볼", TurnResult.BALL);
            memory.remember("슬라이더", TurnResult.BALL);

            BatterBotSwingDecision decision = policy.decide(23, CATALOG, memory);

            assertThat(decision.swing()).isFalse();
            assertThat(decision.batterCoordinateNumber()).isZero();
            assertThat(decision.assumedPitchName()).isEqualTo("커브");
            assertThat(decision.strikeProbability()).isZero();
        }

        @Test
        @DisplayName("제외된 구종은 가정에 쓰이지 않는다")
        void ignoredEliminatedPitch() {
            BatterBotPitchMemory memory = new BatterBotPitchMemory();
            memory.remember("포심 패스트볼", TurnResult.BALL);

            BatterBotSwingDecision decision = policy.decide(13, CATALOG, memory);

            assertThat(decision.assumedPitchName()).isNotEqualTo("포심 패스트볼");
        }
    }
}
