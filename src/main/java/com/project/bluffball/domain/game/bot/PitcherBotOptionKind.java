package com.project.bluffball.domain.game.bot;

/**
 * HARD 투수 봇의 투구 옵션 종류(구종 유형 × 위치 의도).
 */
public enum PitcherBotOptionKind {

    /** 패스트볼, 최종 스트라이크존(카운트 잡기) */
    FASTBALL_STRIKE,

    /** 변화구, 최종 스트라이크존 */
    BREAKING_STRIKE,

    /** 패스트볼, 최종 존 밖(헛스윙 유도) */
    FASTBALL_BALL,

    /** 변화구, 최종 존 밖 */
    BREAKING_BALL
}
