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
    @DisplayName("카탈로그에 없으면 400")
    void notInCatalog() {
        assertThrows(BadRequestException.class, () -> validator.validateInCatalog(false));
    }

    @Test
    @DisplayName("기본본 이미 있으면 409")
    void baseNotPurchasable() {
        assertThrows(ConflictException.class, () -> validator.validatePitchBasePurchasable(false));
    }
}
