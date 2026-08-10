package com.project.bluffball.domain.game.enums;

/**
 * 봇 매치 난이도.
 *
 * <p>덱 구성·의사결정 정책은 난이도별로 달라질 예정이다,
 * 현재는 식별자만 정의한다.</p>
 */
public enum BotDifficulty {

    /** 쉬운 난이도 — 약한 덱·보수적 정책 예정 */
    EASY,

    /** 보통 난이도 — 기본 덱·기본 정책 예정 */
    NORMAL,

    /** 어려운 난이도 — 강한 덱·공격적 정책 예정 */
    HARD
}
