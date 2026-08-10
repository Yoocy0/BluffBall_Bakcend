package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CardHandDrawer")
class CardHandDrawerTest {

    private final CardHandDrawer drawer = new CardHandDrawer();

    @Test
    @DisplayName("풀에서 handSize만큼 뽑는다")
    void drawFromPool() {
        List<Long> pool = List.of(101L, 102L, 103L, 104L);

        List<Long> hand = drawer.draw(pool, 3);

        assertThat(hand).hasSize(3);
        assertThat(pool).containsAll(hand);
    }

    @Test
    @DisplayName("보유 인스턴스가 handSize보다 적으면 GAME_CARD_POOL_INSUFFICIENT")
    void drawFailsWhenPoolTooSmall() {
        assertThatThrownBy(() -> drawer.draw(List.of(101L, 102L), 3))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.GAME_CARD_POOL_INSUFFICIENT);
    }

    @Test
    @DisplayName("재드로우 시 keepIds는 풀에서 제외되어 중복되지 않는다")
    void redrawExcludesKeepIds() {
        List<Long> pool = List.of(101L, 102L, 103L, 104L);
        List<Long> keepIds = List.of(101L);

        List<Long> hand = drawer.redraw(pool, keepIds, 3);

        assertThat(hand).hasSize(3);
        assertThat(hand).contains(101L);
        assertThat(hand.stream().filter(id -> id.equals(101L)).count()).isEqualTo(1L);
        assertThat(hand).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("재드로우 풀이 부족하면 GAME_CARD_REDRAW_INSUFFICIENT")
    void redrawFailsWhenPoolTooSmall() {
        assertThatThrownBy(() -> drawer.redraw(List.of(101L, 102L), List.of(101L), 3))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getErrorCode())
                .isEqualTo(ErrorCode.GAME_CARD_REDRAW_INSUFFICIENT);
    }
}
