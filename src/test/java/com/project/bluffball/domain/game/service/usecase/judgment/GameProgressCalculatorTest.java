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
    @DisplayName("삼진 적용 시 아웃이 늘고 카운트가 리셋된다")
    void applyPromotedStrikeOut() {
        var before = situation(1, 2, 1);
        var transition = calculator.apply(before, TurnResult.STRIKE);
        assertThat(transition.after().outs()).isEqualTo(2);
        assertThat(transition.after().strikes()).isEqualTo(0);
        assertThat(transition.after().balls()).isEqualTo(0);
    }
}
