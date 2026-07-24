package com.project.bluffball.domain.game.config;

import com.project.bluffball.domain.user.record.enums.GameMode;
import org.springframework.stereotype.Component;

/**
 * 게임 모드별 규칙 값을 제공하는 컴포넌트.
 *
 * <p>모드가 추가되거나 규칙 값이 바뀌어도 이 클래스만 수정하면 된다.</p>
 *
 * <h3>투수 카드 핸드 장수</h3>
 * <ul>
 *   <li>SHOWDOWN / CUSTOM — 3장</li>
 *   <li>FULL_LEAGUE / COMPACT_LEAGUE — 5장</li>
 * </ul>
 *
 * <h3>인게임 카드 드로우·멀리건</h3>
 * <ul>
 *   <li>SHOWDOWN / CUSTOM — 셋업 숫자 이후 인게임에서 드로우·멀리건</li>
 *   <li>FULL_LEAGUE / COMPACT_LEAGUE — 매치 전 구종 사전 선택(인게임 드로우·멀리건 없음)</li>
 * </ul>
 *
 * <h3>블러핑 숫자 제출 인원 수</h3>
 * <ul>
 *   <li>전 모드 — 2명 (추후 모드별 확장 가능)</li>
 * </ul>
 */
@Component
public class GameModeRule {

    // 게임 모드에 따른 초기 드로우 장수 반환 메서드
    public int getHandSize(GameMode gameMode) {
        return switch (gameMode) {
            case SHOWDOWN, CUSTOM -> 3;
            case FULL_LEAGUE, COMPACT_LEAGUE -> 5;
        };
    }

    /**
     * 셋업 숫자 완료 후 인게임에서 구종 카드 드로우·멀리건을 진행하는 모드인지.
     *
     * <p>리그 모드는 매치 시작 전 구종을 사전 선택하므로 false.</p>
     */
    public boolean usesInGameCardDrawAndMulligan(GameMode gameMode) {
        return switch (gameMode) {
            case SHOWDOWN, CUSTOM -> true;
            case FULL_LEAGUE, COMPACT_LEAGUE -> false;
        };
    }

    /** setup-numbers 완료로 판정하기 위해 필요한 제출 인원 수 */
    public int getRequiredSetupCount(GameMode gameMode) {
        return 2;
    }

    /** 모드별 기본 총 이닝 수 — 커스텀 모드는 매치 설정값을 직접 전달한다. */
    public int getDefaultInnings(GameMode gameMode) {
        return switch (gameMode) {
            case SHOWDOWN, CUSTOM -> 1;
            case COMPACT_LEAGUE -> 3;
            case FULL_LEAGUE -> 9;
        };
    }
}
