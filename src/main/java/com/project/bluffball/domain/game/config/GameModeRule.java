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
 *   <li>GENERAL(싱글) — 3장</li>
 *   <li>CLAN_MINI / CLAN_GENERAL(팀전) — 5장</li>
 *   <li>CUSTOM — 3장 (기본값)</li>
 * </ul>
 *
 * <h3>블러핑 숫자 제출 인원 수</h3>
 * <ul>
 *   <li>GENERAL(싱글) — 2명 (home + away)</li>
 *   <li>CLAN_MINI / CLAN_GENERAL / CUSTOM — 2명 (팀 대표)</li>
 * </ul>
 */
@Component
public class GameModeRule {

    // 게임 모드에 따른 초기 드로우 장수 반환 메서드
    public int getHandSize(GameMode gameMode) {
        return switch (gameMode) {
            case GENERAL, CUSTOM -> 3;
            case CLAN_MINI, CLAN_GENERAL -> 5;
        };
    }

    /** setup-numbers 완료로 판정하기 위해 필요한 제출 인원 수 */
    public int getRequiredSetupCount(GameMode gameMode) {
        return 2;
    }

    /** 모드별 기본 총 이닝 수 — 커스텀 모드는 매치 설정값을 직접 전달한다. */
    public int getDefaultInnings(GameMode gameMode) {
        return switch (gameMode) {
            case GENERAL -> 1;
            case CLAN_MINI -> 3;
            case CLAN_GENERAL -> 9;
            case CUSTOM -> 1;
        };
    }
}
