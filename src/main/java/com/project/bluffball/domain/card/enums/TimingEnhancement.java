package com.project.bluffball.domain.card.enums;

/**
 * 구종 타이밍 강화 상태.
 * DB에는 ordinal(0=NONE, 1=FASTER, 2=SLOWER) 정수로 저장된다.
 */
public enum TimingEnhancement {
    /** 강화 없음 — 마스터 타이밍 그대로 */
    NONE,
    /** 한 칸 더 빠른 타이밍 (ordinal −1) */
    FASTER,
    /** 한 칸 더 느린 타이밍 (ordinal +1) */
    SLOWER
}
