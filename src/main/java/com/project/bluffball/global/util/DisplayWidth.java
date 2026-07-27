package com.project.bluffball.global.util;

/**
 * UI 표시 폭 계산 유틸.
 *
 * <p>한글·CJK·전각은 2, 그 외(영문·숫자·반각 기호 등)는 1로 센다.
 * 닉네임·팀 이름 공통 상한은 {@link #MAX_DISPLAY_NAME_WIDTH}(한글 8자).</p>
 */
public final class DisplayWidth {

    /** 닉네임·팀 이름 공통 표시 폭 상한 (한글 8자 / 영문 16자) */
    public static final int MAX_DISPLAY_NAME_WIDTH = 16;

    private DisplayWidth() {
    }

    /**
     * 문자열의 표시 폭을 계산한다.
     *
     * @param value 대상 문자열 (null이면 0)
     * @return 표시 폭
     */
    public static int of(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        int width = 0;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            width += isWide(codePoint) ? 2 : 1;
            i += Character.charCount(codePoint);
        }
        return width;
    }

    /**
     * 표시 폭이 상한 이하인지 확인한다.
     *
     * @param value 대상 문자열
     * @param maxWidth 최대 표시 폭
     * @return 이하면 true
     */
    public static boolean isWithin(String value, int maxWidth) {
        return of(value) <= maxWidth;
    }

    /**
     * 동아시아 전각·한글·한자 계열인지.
     *
     * @param codePoint 코드포인트
     * @return 넓은 문자면 true
     */
    private static boolean isWide(int codePoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        if (block == null) {
            return false;
        }
        return block == Character.UnicodeBlock.HANGUL_SYLLABLES
                || block == Character.UnicodeBlock.HANGUL_JAMO
                || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA;
    }
}
