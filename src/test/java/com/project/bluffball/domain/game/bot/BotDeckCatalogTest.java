package com.project.bluffball.domain.game.bot;

import com.project.bluffball.domain.game.enums.BotDifficulty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BotDeckCatalog")
class BotDeckCatalogTest {

    private final BotDeckCatalog catalog = new BotDeckCatalog();

    @Test
    @DisplayName("placeholder 덱은 난이도별로 비어 있다")
    void placeholderDecksEmpty() {
        for (BotDifficulty difficulty : BotDifficulty.values()) {
            assertThat(catalog.masterPitchCardIdsFor(difficulty)).isEmpty();
        }
    }

    @Test
    @DisplayName("null 난이도는 빈 목록")
    void nullDifficulty() {
        assertThat(catalog.masterPitchCardIdsFor(null)).isEmpty();
    }
}
