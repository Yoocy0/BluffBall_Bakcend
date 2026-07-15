package com.project.bluffball.domain.user.record.entity;

import com.project.bluffball.domain.user.record.enums.GameMode;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투수 상세 성적 엔터티 (record_type_code = 1).
 */
@Entity
@DiscriminatorValue("1")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PitcherRecord extends UserRecord {

    /** 총 소화 이닝 수 */
    @Column(name = "innings", nullable = false)
    private int innings;

    /** 이닝 미만의 아웃 카운트 수 (0 ~ 2) */
    @Column(name = "out_counts", nullable = false)
    private int outCounts;

    /** 탈삼진 */
    @Column(name = "strike_outs", nullable = false)
    private int strikeOuts;

    /** 볼넷 */
    @Column(name = "base_on_balls", nullable = false)
    private int baseOnBalls;

    /** 피안타 */
    @Column(name = "hits", nullable = false)
    private int hits;

    /** 자책점 */
    @Column(name = "earned_runs", nullable = false)
    private int earnedRuns;

    /** 피 2루타 */
    @Column(name = "doubles_allowed", nullable = false)
    private int doublesAllowed;

    /** 피 3루타 */
    @Column(name = "triples_allowed", nullable = false)
    private int triplesAllowed;

    /** 피홈런 */
    @Column(name = "home_runs_allowed", nullable = false)
    private int homeRunsAllowed;

    /**
     * 아웃 카운트를 누적하고, 3개가 쌓일 때마다 이닝을 자동으로 증가시킨다.
     *
     * <p>예시: innings = 2, outCounts = 1인 상태에서 additionalOuts = 5 입력 시
     * totalOuts = 6 → innings = 2 + 2 = 4, outCounts = 0</p>
     *
     * @param additionalOuts 추가할 아웃 카운트 (0 이상의 정수)
     * @throws BadRequestException additionalOuts가 음수인 경우
     */
    public void addOutCounts(int additionalOuts) {
        if (additionalOuts < 0) {
            throw new BadRequestException(ErrorCode.PITCHER_RECORD_INVALID, "additionalOuts=" + additionalOuts);
        }
        int totalOuts = this.outCounts + additionalOuts;
        this.innings += (totalOuts / 3);
        this.outCounts = (totalOuts % 3);
    }

    public PitcherRecord(Long userId, GameMode gameMode, int totalGames, int wins, int loses,
                        int innings, int outCounts, int strikeOuts, int baseOnBalls,
                        int hits, int earnedRuns, int doublesAllowed, int triplesAllowed,
                        int homeRunsAllowed) {
        super(userId, gameMode, totalGames, wins, loses);
        this.innings = innings;
        this.outCounts = outCounts;
        this.strikeOuts = strikeOuts;
        this.baseOnBalls = baseOnBalls;
        this.hits = hits;
        this.earnedRuns = earnedRuns;
        this.doublesAllowed = doublesAllowed;
        this.triplesAllowed = triplesAllowed;
        this.homeRunsAllowed = homeRunsAllowed;
    }
}
