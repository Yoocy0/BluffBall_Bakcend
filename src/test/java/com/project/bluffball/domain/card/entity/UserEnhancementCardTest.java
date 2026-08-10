package com.project.bluffball.domain.card.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("UserEnhancementCard")
class UserEnhancementCardTest {

    @Test
    @DisplayName("수량 추가·소모")
    void addAndConsume() {
        UserEnhancementCard owned = new UserEnhancementCard(1L, 10L, 2);
        owned.addQuantity(1);
        assertEquals(3, owned.getQuantity());
        owned.consumeOne();
        assertEquals(2, owned.getQuantity());
    }

    @Test
    @DisplayName("수량 0에서 소모 시 예외")
    void consumeWhenEmpty() {
        UserEnhancementCard owned = new UserEnhancementCard(1L, 10L, 1);
        owned.consumeOne();
        assertThrows(IllegalStateException.class, owned::consumeOne);
    }
}
