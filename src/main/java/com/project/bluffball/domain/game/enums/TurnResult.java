package com.project.bluffball.domain.game.enums;

/**
 * 한 턴(투구 및 타석)의 최종 판정 결과.
 * DB에는 ordinal(0~11) 정수로 저장된다.
 * value는 ordinal과 항상 일치해야 한다.
 */
public enum TurnResult {
    STRIKE(0),          // 스트라이크 (공 하나 단위)
    BALL(1),            // 볼 (공 하나 단위)
    SINGLE(2),          // 1루타
    DOUBLE(3),          // 2루타
    TRIPLE(4),          // 3루타
    HOMERUN(5),         // 홈런
    WALK(6),            // 볼넷 (사구 없음)
    STRIKE_OUT(7),      // 삼진 아웃
    OUT(8),             // 일반 아웃 (땅볼/플라이 구분 없음)
    DOUBLE_PLAY(9),     // 병살타
    WILD_PITCH(10),     // 폭투 (MVP — 추후 주자 진루 등 확장 예정)
    FOUL(11);           // 파울 (스트라이크 가산, 단 2S에서는 카운트 유지)

    private final int value;

    TurnResult(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
