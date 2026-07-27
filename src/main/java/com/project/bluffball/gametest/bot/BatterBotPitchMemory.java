package com.project.bluffball.gametest.bot;

import com.project.bluffball.domain.game.enums.TurnResult;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 타자 봇이 상대가 던진 구종을 기억하는 상태.
 *
 * <p>이미 던진 구종 중 스트라이크가 되지 않은 구종은 이후 후보에서 제외한다.</p>
 */
public final class BatterBotPitchMemory {

    /** 스트라이크가 되지 않아 후보에서 뺀 구종명 */
    private final Set<String> eliminatedPitchNames = new HashSet<>();

    /**
     * 턴 결과로 구종 후보를 갱신한다.
     *
     * <p>스트라이크·삼진이 아니면 해당 구종을 후보에서 제거한다.</p>
     *
     * @param pitchCardName 이번 투구 구종명
     * @param turnResult 턴 판정
     */
    public void remember(String pitchCardName, TurnResult turnResult) {
        if (pitchCardName == null || pitchCardName.isBlank() || turnResult == null) {
            return;
        }
        if (turnResult != TurnResult.STRIKE && turnResult != TurnResult.STRIKE_OUT) {
            eliminatedPitchNames.add(pitchCardName);
        }
    }

    /**
     * 제외된 구종인지.
     *
     * @param pitchCardName 구종명
     * @return 제외면 true
     */
    public boolean isEliminated(String pitchCardName) {
        return pitchCardName != null && eliminatedPitchNames.contains(pitchCardName);
    }

    /**
     * 제외 구종명 스냅샷.
     *
     * @return 수정 불가 집합
     */
    public Set<String> eliminatedPitchNames() {
        return Collections.unmodifiableSet(eliminatedPitchNames);
    }

    /**
     * 기억을 초기화한다. (새 매치 시)
     */
    public void clear() {
        eliminatedPitchNames.clear();
    }
}
