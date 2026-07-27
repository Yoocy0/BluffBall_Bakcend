package com.project.bluffball.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DisplayWidth")
class DisplayWidthTest {

    @Test
    @DisplayName("영문은 1폭으로 센다")
    void asciiWidth() {
        assertThat(DisplayWidth.of("Player_ab")).isEqualTo(9);
        assertThat(DisplayWidth.isWithin("a".repeat(16), DisplayWidth.MAX_DISPLAY_NAME_WIDTH)).isTrue();
        assertThat(DisplayWidth.isWithin("a".repeat(17), DisplayWidth.MAX_DISPLAY_NAME_WIDTH)).isFalse();
    }

    @Test
    @DisplayName("한글은 2폭으로 센다")
    void hangulWidth() {
        assertThat(DisplayWidth.of("블러프볼팀")).isEqualTo(10);
        assertThat(DisplayWidth.isWithin("가나다라마바사아", DisplayWidth.MAX_DISPLAY_NAME_WIDTH)).isTrue();
        assertThat(DisplayWidth.isWithin("가나다라마바사아자", DisplayWidth.MAX_DISPLAY_NAME_WIDTH)).isFalse();
    }

    @Test
    @DisplayName("혼합 문자열 폭을 합산한다")
    void mixed() {
        // 한글 2자(4) + 영문 2자(2) = 6
        assertThat(DisplayWidth.of("한글AB")).isEqualTo(6);
    }
}
