package com.project.bluffball.domain.game.entity;

import com.project.bluffball.domain.game.enums.TurnResult;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 이닝별 투구 로그 영구 저장 엔터티.
 * 개발/기획의 인게임 밸런싱 분석 및 유저 민원 보상 확인 목적으로만 사용된다.
 *
 * <p>경기 종료 시 Redis의 TurnResultSession 객체들을 matchSessionId로 일괄 조회하여
 * 변환 저장한다. 생성 후 90일 경과 시 스케줄러에 의해 자동 삭제된다.</p>
 */
@Entity
@Table(name = "inning_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InningLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inning_log_id")
    private Long id;

    /** 소속 매치 세션 식별 꼬리표 (중복 허용) */
    @Column(name = "match_session_id", nullable = false)
    private String matchSessionId;

    /** 턴 번호 (경기 내 투구 순서) */
    @Column(name = "turn_number", nullable = false)
    private int turnNumber;

    /** 해당 턴에 공을 던진 투수 유저 ID */
    @Column(name = "pitcher_user_id", nullable = false)
    private Long pitcherUserId;

    /** 해당 턴에 타석에 선 타자 유저 ID */
    @Column(name = "batter_user_id", nullable = false)
    private Long batterUserId;

    /** 이닝 수 */
    @Column(name = "inning", nullable = false)
    private int inning;

    /** 이닝 초/말 구분 (true: 초, false: 말) */
    @Column(name = "is_top", nullable = false)
    private boolean isTop;

    /** 투수가 낸 구종 카드 ID */
    @Column(name = "selected_pitch_card_id", nullable = false)
    private Long selectedPitchCardId;

    /** 투수가 낸 좌표 카드 ID */
    @Column(name = "selected_coordinate_card_id", nullable = false)
    private Long selectedCoordinateCardId;

    /** 타자가 예측하여 선택한 최종 좌표 카드 ID */
    @Column(name = "selected_batter_coordinate_card_id", nullable = false)
    private Long selectedBatterCoordinateCardId;

    /** 타자가 낸 타이밍 카드 ID */
    @Column(name = "selected_timing_card_id", nullable = false)
    private Long selectedTimingCardId;

    /** 이 턴의 최종 판정 결과 — DB에 ordinal 정수로 저장 */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "turn_result", nullable = false)
    private TurnResult turnResult;

    /** 레코드 생성 시각 — 90일 후 삭제 스케줄링 기준 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public InningLog(String matchSessionId, int turnNumber,
                     Long pitcherUserId, Long batterUserId,
                     int inning, boolean isTop,
                     Long selectedPitchCardId, Long selectedCoordinateCardId,
                     Long selectedBatterCoordinateCardId, Long selectedTimingCardId,
                     TurnResult turnResult) {
        this.matchSessionId = matchSessionId;
        this.turnNumber = turnNumber;
        this.pitcherUserId = pitcherUserId;
        this.batterUserId = batterUserId;
        this.inning = inning;
        this.isTop = isTop;
        this.selectedPitchCardId = selectedPitchCardId;
        this.selectedCoordinateCardId = selectedCoordinateCardId;
        this.selectedBatterCoordinateCardId = selectedBatterCoordinateCardId;
        this.selectedTimingCardId = selectedTimingCardId;
        this.turnResult = turnResult;
    }
}
