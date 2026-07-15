package com.project.bluffball.domain.game.enums;

/**
 * 모바일 재접속 시 복원할 인게임 단계.
 */
public enum GameSessionPhase {
    /** 블러핑 숫자 제출 대기 */
    SETUP_NUMBERS,
    /** 카드 교체(멀리건) 단계 */
    MULLIGAN,
    /** 투수 카드·시작 좌표 선택 */
    PITCHER_SELECT,
    /** 타자 좌표·타이밍 선택 */
    BATTER_SELECT,
    /** 경기 종료 */
    ENDED
}
