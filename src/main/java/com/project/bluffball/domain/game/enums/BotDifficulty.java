package com.project.bluffball.domain.game.enums;

/**
 * 봇 매치 난이도.
 *
 * <p>드로우 풀은 난이도별로 다르다(EASY 기본 4장 / NORMAL 전체 기본본 /
 * HARD 기본본+단일 강화). 의사결정 AI 정책은 별도 과제다.</p>
 */
public enum BotDifficulty {

    /** 쉬운 난이도 — 튜토리얼 기본 덱 4장(미강화) */
    EASY,

    /** 보통 난이도 — 전체 마스터 기본본 */
    NORMAL,

    /** 어려운 난이도 — 전체 마스터 기본본 + 합법 단일 강화 변형 */
    HARD
}
