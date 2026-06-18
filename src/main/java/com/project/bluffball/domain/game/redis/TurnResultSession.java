package com.project.bluffball.domain.game.redis;

import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.game.enums.TurnResult;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.List;

/**
 * Redis 저장용 턴 단위 결과 세션 객체.
 * JPA 엔터티가 아니며, Spring Data Redis의 @RedisHash로 관리된다.
 *
 * <p>유일성과 턴의 연속성을 완벽히 보장하기 위해 "매치ID:턴번호" 조합 문자열을 @Id로 사용한다.
 * 경기 최종 종료 시 matchSessionId로 해당 경기의 모든 TurnResultSession을 일괄 조회하여
 * MariaDB에 InningLog로 변환 저장한 뒤 삭제한다.</p>
 *
 * <p>Redis 저장 구조</p>
 * <pre>
 * KEY   : TurnResultSession:{matchSessionId}:{turnNumber}
 * FIELD : matchSessionId, turnNumber, inning, isTop,
 *         currentPitcherUserId, currentBatterUserId,
 *         selectedPitchCardId, selectedCoordinateCardId,
 *         startCoordinateNumber, finalCoordinateNumber, pitchTiming,
 *         selectedBatterCoordinateCardId, selectedTiming,
 *         turnResult
 * </pre>
 */
@RedisHash(value = "TurnResultSession", timeToLive = 3600)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TurnResultSession {

    /**
     * Redis Key — "매치ID:턴번호" 조합 문자열.
     * 예: "550e8400-e29b-41d4-a716-446655440000:1"
     */
    @Id
    private String id;

    /** 소속 매치 세션 ID — 경기 종료 시 일괄 조회 키 (Redis Set "MATCH_TURN_IDS:{matchSessionId}"로 관리) */
    private String matchSessionId;

    /** 턴 번호 (경기 내 투구 순서) */
    private int turnNumber;

    /** 해당 턴의 이닝 수 */
    private int inning;

    /** 이닝 초/말 구분 (true: 초, false: 말) */
    private boolean isTop;

    /** 현재 공을 던진 투수 유저 ID */
    private Long currentPitcherUserId;

    /** 현재 타석에 선 타자 유저 ID */
    private Long currentBatterUserId;

    /** 투수가 낸 구종 카드 ID */
    private Long selectedPitchCardId;

    /** 투수가 낸 좌표 카드 ID */
    private Long selectedCoordinateCardId;

    /** 투수가 선택한 시작 좌표 번호 (1~25) */
    private int startCoordinateNumber;

    /**
     * 구종 변화 적용 후 투수 공의 최종 좌표 번호 (1~25, 격자 이탈 시 0).
     * 투수 선택 직후 {@link com.project.bluffball.domain.card.entity.PitchCard#calculateFinalCoordinateNumber}로 확정·저장한다.
     */
    private int finalCoordinateNumber;

    /**
     * 선택한 구종 카드의 고유 타이밍 — Redis에 ordinal 정수로 저장.
     * 타자 선택 후 판정 시 {@link #selectedTiming}과 비교한다.
     */
    @Enumerated(EnumType.ORDINAL)
    private Timing pitchTiming;

    /** 타자가 예측하여 선택한 최종 좌표 번호 (0 = 폭투 존) */
    private int batterSelectedCoordinateNumber;

    /** 타자가 예측하여 선택한 최종 좌표 카드 ID (레거시·선택) */
    private Long selectedBatterCoordinateCardId;

    /**
     * 타자가 선택한 타이밍 칸 — Redis에 ordinal 정수로 저장.
     * 타임아웃 또는 스윙 미발동 시 null.
     */
    @Enumerated(EnumType.ORDINAL)
    private Timing selectedTiming;

    /**
     * 이 턴에 부여된 주사위 눈금 결과 목록 — 주사위마다 1~6.
     * 완벽 일치: 2개(합 2~12), 빗맞음: 1개(합 1~6), 헛스윙/타임아웃: 빈 리스트.
     */
    private List<Integer> diceResults;

    /** 이 턴의 최종 판정 결과 — Redis에 ordinal 정수로 저장 */
    @Enumerated(EnumType.ORDINAL)
    private TurnResult turnResult;

    @Builder
    public TurnResultSession(String matchSessionId, int turnNumber, int inning, boolean isTop,
                             Long currentPitcherUserId, Long currentBatterUserId,
                             Long selectedPitchCardId, Long selectedCoordinateCardId,
                             int startCoordinateNumber, int finalCoordinateNumber, Timing pitchTiming,
                             Long selectedBatterCoordinateCardId, Timing selectedTiming,
                             List<Integer> diceResults, TurnResult turnResult) {
        this.id = matchSessionId + ":" + turnNumber;
        this.matchSessionId = matchSessionId;
        this.turnNumber = turnNumber;
        this.inning = inning;
        this.isTop = isTop;
        this.currentPitcherUserId = currentPitcherUserId;
        this.currentBatterUserId = currentBatterUserId;
        this.selectedPitchCardId = selectedPitchCardId;
        this.selectedCoordinateCardId = selectedCoordinateCardId;
        this.startCoordinateNumber = startCoordinateNumber;
        this.finalCoordinateNumber = finalCoordinateNumber;
        this.pitchTiming = pitchTiming;
        this.selectedBatterCoordinateCardId = selectedBatterCoordinateCardId;
        this.selectedTiming = selectedTiming;
        this.diceResults = diceResults;
        this.turnResult = turnResult;
    }

    /** 타자 선택 및 판정 결과를 반영한다. BatterCardSelectExecutor에서만 호출한다. */
    public void applyBatterTurn(int batterSelectedCoordinateNumber,
                                Timing selectedTiming,
                                List<Integer> diceResults,
                                TurnResult turnResult) {
        this.batterSelectedCoordinateNumber = batterSelectedCoordinateNumber;
        this.selectedTiming = selectedTiming;
        this.diceResults = diceResults;
        this.turnResult = turnResult;
    }
}
