package com.project.bluffball.domain.game.service.usecase.judgment;

import com.project.bluffball.domain.game.dto.progress.GameProgressSituation;
import com.project.bluffball.domain.game.enums.TurnResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameProgressCalculatorTest {

    private final GameProgressCalculator calculator = new GameProgressCalculator();

    private static GameProgressSituation situation(int balls, int strikes, int outs) {
        return new GameProgressSituation(
                3, 1, true,
                0, 0,
                balls, strikes, outs,
                false, false, false);
    }

    @Test
    @DisplayName("2S에서 STRIKE는 STRIKE_OUT으로 승격된다")
    void promoteThirdStrike() {
        TurnResult effective = calculator.resolveEffectiveTurnResult(
                TurnResult.STRIKE, situation(0, 2, 0));
        assertThat(effective).isEqualTo(TurnResult.STRIKE_OUT);
    }

    @Test
    @DisplayName("3B에서 BALL은 WALK으로 승격된다")
    void promoteFourthBall() {
        TurnResult effective = calculator.resolveEffectiveTurnResult(
                TurnResult.BALL, situation(3, 0, 0));
        assertThat(effective).isEqualTo(TurnResult.WALK);
    }

    @Test
    @DisplayName("3B에서 WILD_PITCH도 WALK으로 승격된다")
    void promoteWildPitchWalk() {
        TurnResult effective = calculator.resolveEffectiveTurnResult(
                TurnResult.WILD_PITCH, situation(3, 1, 0));
        assertThat(effective).isEqualTo(TurnResult.WALK);
    }

    @Test
    @DisplayName("중간 카운트 STRIKE/BALL은 승격되지 않는다")
    void keepIntermediateCounts() {
        assertThat(calculator.resolveEffectiveTurnResult(TurnResult.STRIKE, situation(1, 1, 0)))
                .isEqualTo(TurnResult.STRIKE);
        assertThat(calculator.resolveEffectiveTurnResult(TurnResult.BALL, situation(2, 0, 0)))
                .isEqualTo(TurnResult.BALL);
    }

    @Test
    @DisplayName("파울은 2S에서 카운트를 유지한다")
    void foulDoesNotPromoteAtTwoStrikes() {
        assertThat(calculator.resolveEffectiveTurnResult(TurnResult.FOUL, situation(0, 2, 0)))
                .isEqualTo(TurnResult.FOUL);

        var transition = calculator.apply(situation(1, 2, 0), TurnResult.FOUL);
        assertThat(transition.after().strikes()).isEqualTo(2);
        assertThat(transition.after().balls()).isEqualTo(1);
        assertThat(transition.after().outs()).isEqualTo(0);
    }

    @Test
    @DisplayName("0~1S에서 파울은 스트라이크를 1 올린다")
    void foulAddsStrikeBeforeTwo() {
        var fromZero = calculator.apply(situation(0, 0, 0), TurnResult.FOUL);
        assertThat(fromZero.after().strikes()).isEqualTo(1);

        var fromOne = calculator.apply(situation(0, 1, 0), TurnResult.FOUL);
        assertThat(fromOne.after().strikes()).isEqualTo(2);
    }
}
