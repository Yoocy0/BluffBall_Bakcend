package com.project.bluffball.domain.tutorial.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("TutorialCompleteValidator")
class TutorialCompleteValidatorTest {

    private final TutorialCompleteValidator validator = new TutorialCompleteValidator();

    @Nested
    @DisplayName("validateNotCompleted")
    class ValidateNotCompleted {

        @Test
        @DisplayName("미완료면 통과")
        void okWhenNotCompleted() {
            assertDoesNotThrow(() -> validator.validateNotCompleted(false));
        }

        @Test
        @DisplayName("완료면 409")
        void conflictWhenCompleted() {
            assertThrows(ConflictException.class, () -> validator.validateNotCompleted(true));
        }
    }

    @Nested
    @DisplayName("validateStarterSelection")
    class ValidateStarterSelection {

        @Test
        @DisplayName("커브·슬라이더 선택 통과")
        void okCurveSlider() {
            assertDoesNotThrow(() -> validator.validateStarterSelection(
                    List.of(1L, 2L), List.of("커브", "슬라이더")));
        }

        @Test
        @DisplayName("포심 포함 시 실패")
        void rejectFixedPitch() {
            assertThrows(BadRequestException.class, () -> validator.validateStarterSelection(
                    List.of(1L, 2L), List.of("포심 패스트볼", "커브")));
        }

        @Test
        @DisplayName("중복 선택 실패")
        void rejectDuplicate() {
            assertThrows(BadRequestException.class, () -> validator.validateStarterSelection(
                    List.of(1L, 1L), List.of("커브", "커브")));
        }

        @Test
        @DisplayName("1개만 선택 실패")
        void rejectSingle() {
            assertThrows(BadRequestException.class, () -> validator.validateStarterSelection(
                    List.of(1L), List.of("커브")));
        }
    }
}
