package com.project.bluffball.domain.card.enums;

/**
 * 강화 카드 효과.
 *
 * <p>DB에는 ordinal 정수로 저장한다. 분기 키는 cardId가 아니라 이 enum이다.</p>
 */
public enum EnhancementEffect {
    /** 변화량 +1 */
    CHANGE_AMOUNT_PLUS_1,
    /** 타이밍 1단계 빠름 */
    TIMING_FASTER,
    /** 타이밍 1단계 느림 */
    TIMING_SLOWER
}
