package com.project.bluffball.domain.store.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("StoreValidator")
class StoreValidatorTest {

    private final StoreValidator validator = new StoreValidator();

    @Test
    @DisplayName("재화 부족 시 400")
    void currencyInsufficient() {
        assertThrows(BadRequestException.class, () -> validator.validateCurrencyEnough(50, 100));
    }

    @Test
    @DisplayName("재화 충분하면 통과")
    void currencyEnough() {
        assertDoesNotThrow(() -> validator.validateCurrencyEnough(100, 100));
    }

    @Test
    @DisplayName("이미 보유하면 409")
    void alreadyOwned() {
        assertThrows(ConflictException.class, () -> validator.validateNotOwned(true));
    }

    @Test
    @DisplayName("오늘 오퍼가 아니면 400")
    void notOffered() {
        assertThrows(BadRequestException.class, () -> validator.validateOfferedToday(false));
    }
}
