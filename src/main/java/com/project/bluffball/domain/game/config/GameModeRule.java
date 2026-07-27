package com.project.bluffball.domain.game.config;

import com.project.bluffball.domain.user.record.enums.GameMode;
import org.springframework.stereotype.Component;

/**
 * 게임 모드별 규칙 값을 제공하는 컴포넌트.
 *
 * <p>모드가 추가되거나 규칙 값이 바뀌어도 이 클래스만 수정하면 된다.</p>
 *
 * <h3>투수 카드 핸드 장수 (리그 = 사전 선택 n+1, 구종·강화 포함)</h3>
 * <ul>
 *   <li>SHOWDOWN / CUSTOM — 3장 (인게임 드로우)</li>
 *   <li>COMPACT_LEAGUE — 4장 (사전 선택 = 경기 핸드)</li>
 *   <li>FULL_LEAGUE — 5장 (사전 선택 = 경기 핸드)</li>
 * </ul>
 *
 * <h3>투수 교체 시 핸드</h3>
 * <ul>
 *   <li>리그 — 사전 지정한 +1장이 빠지고 남은 n장 사용 (Compact 3 / Full 4)</li>
 * </ul>
 *
 * <h3>인게임 카드 드로우·멀리건</h3>
 * <ul>
 *   <li>SHOWDOWN / CUSTOM — 셋업 숫자 이후 인게임에서 드로우·멀리건</li>
 *   <li>FULL_LEAGUE / COMPACT_LEAGUE — 매치 전 구종·강화 사전 선택(인게임 드로우·멀리건 없음)</li>
 * </ul>
 *
 * <h3>출전 로스터 / 투수 교체 한도</h3>
 * <ul>
 *   <li>COMPACT_LEAGUE — 타순 3명 + 전담 투수 1명(총 4명), 투수 교체 1회</li>
 *   <li>FULL_LEAGUE — 타순 9명(선발 투수는 타순에 포함), 투수 교체 3회</li>
 * </ul>
 *
 * <h3>블러핑 숫자 제출</h3>
 * <ul>
 *   <li>SHOWDOWN — 참가 2명 FULL(OUT·병살·3루타·홈런)</li>
 *   <li>리그 — 타순 멤버 타자 셋업 + 양 선발/현재 투수 셋업.
 *       Compact 전담 투수는 타자 셋업 불필요. 투수 교체 시에만 역할별 재제출</li>
 * </ul>
 */
@Component
public class GameModeRule {

    /**
     * 경기 구종 핸드 장수(n+1). 리그는 사전 선택 장수와 동일하다.
     *
     * @param gameMode 게임 모드
     * @return 핸드 장수
     */
    public int getHandSize(GameMode gameMode) {
        return switch (gameMode) {
            case SHOWDOWN, CUSTOM -> 3;
            case COMPACT_LEAGUE -> 4;
            case FULL_LEAGUE -> 5;
        };
    }

    /**
     * 투수 교체 후 사용 가능한 핸드 장수(n = handSize − 1).
     *
     * <p>쇼다운은 투수 교체가 없으므로 handSize와 동일하게 반환한다.</p>
     *
     * @param gameMode 게임 모드
     * @return 교체 투수 핸드 장수
     */
    public int getSubstituteHandSize(GameMode gameMode) {
        return switch (gameMode) {
            case SHOWDOWN, CUSTOM -> getHandSize(gameMode);
            case COMPACT_LEAGUE, FULL_LEAGUE -> getHandSize(gameMode) - 1;
        };
    }

    /**
     * 셋업 숫자 완료 후 인게임에서 구종 카드 드로우·멀리건을 진행하는 모드인지.
     *
     * <p>리그 모드는 매치 시작 전 구종·강화를 사전 선택하므로 false.</p>
     *
     * @param gameMode 게임 모드
     * @return 인게임 드로우·멀리건 사용 여부
     */
    public boolean usesInGameCardDrawAndMulligan(GameMode gameMode) {
        return switch (gameMode) {
            case SHOWDOWN, CUSTOM -> true;
            case FULL_LEAGUE, COMPACT_LEAGUE -> false;
        };
    }

    /**
     * setup-numbers 완료로 판정하기 위해 필요한 제출 인원 수.
     *
     * @param gameMode 게임 모드
     * @return 필요 제출 인원
     */
    public int getRequiredSetupCount(GameMode gameMode) {
        return 2;
    }

    /**
     * 모드별 기본 총 이닝 수 — 커스텀 모드는 매치 설정값을 직접 전달한다.
     *
     * @param gameMode 게임 모드
     * @return 이닝 수
     */
    public int getDefaultInnings(GameMode gameMode) {
        return switch (gameMode) {
            case SHOWDOWN, CUSTOM -> 1;
            case COMPACT_LEAGUE -> 3;
            case FULL_LEAGUE -> 9;
        };
    }

    /**
     * 타순(타자) 인원 수.
     *
     * <p>Compact는 타자 3명, Full은 9명이다. 선발 투수는 Compact에서 별도 인원이다.</p>
     *
     * @param gameMode 게임 모드
     * @return 타순 인원
     */
    public int getBattingOrderSize(GameMode gameMode) {
        return switch (gameMode) {
            case SHOWDOWN, CUSTOM -> 1;
            case COMPACT_LEAGUE -> 3;
            case FULL_LEAGUE -> 9;
        };
    }

    /**
     * 출전 전원 인원 수(구종 사전선택·팀 최소 인원 기준).
     *
     * <p>Compact = 타자 3 + 전담 투수 1. Full = 타순 9(투수 겸임).</p>
     *
     * @param gameMode 게임 모드
     * @return 출전 전원 인원
     */
    public int getRosterSize(GameMode gameMode) {
        return switch (gameMode) {
            case SHOWDOWN, CUSTOM -> 1;
            case COMPACT_LEAGUE -> 4;
            case FULL_LEAGUE -> 9;
        };
    }

    /**
     * 선발 투수가 타순과 별도 인원인지(Compact).
     *
     * @param gameMode 게임 모드
     * @return Compact면 true
     */
    public boolean hasDedicatedPitcher(GameMode gameMode) {
        return gameMode == GameMode.COMPACT_LEAGUE;
    }

    /**
     * 경기 중 투수 교체 최대 횟수.
     *
     * @param gameMode 게임 모드
     * @return 교체 한도 (쇼다운은 0)
     */
    public int getMaxPitcherSubstitutions(GameMode gameMode) {
        return switch (gameMode) {
            case SHOWDOWN, CUSTOM -> 0;
            case COMPACT_LEAGUE -> 1;
            case FULL_LEAGUE -> 3;
        };
    }
}
