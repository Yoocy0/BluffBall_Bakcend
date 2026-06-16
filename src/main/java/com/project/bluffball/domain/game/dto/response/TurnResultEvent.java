package com.project.bluffball.domain.game.dto.response;

import com.project.bluffball.domain.game.enums.TurnResult;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 턴 결과 WebSocket 송신 이벤트.
 * 양측 선택 완료 후 결과 계산이 끝나면 투수와 타자 모두에게 전송된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TurnResultEvent {

    /** 이 턴의 최종 판정 결과 */
    private TurnResult turnResult;

    /** 투수 공의 실제 최종 좌표 번호 (결과 공개 / 0 = 폭투) */
    private int finalCoordinateNumber;

    /**
     * 이 턴에 굴린 주사위 눈금 결과 목록 (1~12).
     * 완벽 일치 시 2개, 빗맞음(1칸 어긋남) 시 1개, 헛스윙/타임아웃 시 빈 리스트.
     * 클라이언트의 주사위 굴리기 애니메이션 연출에 사용된다.
     */
    private List<Integer> diceResults;

    /** 현재 이닝 수 */
    private int inning;

    /** 이닝 초/말 (true: 초, false: 말) */
    private boolean isTop;

    /** 홈팀 점수 */
    private int homeScore;

    /** 어웨이팀 점수 */
    private int awayScore;

    /** 볼 카운트 */
    private int balls;

    /** 스트라이크 카운트 */
    private int strikes;

    /** 아웃 카운트 */
    private int outs;

    /** 1루 주자 유무 */
    private boolean firstBase;

    /** 2루 주자 유무 */
    private boolean secondBase;

    /** 3루 주자 유무 */
    private boolean thirdBase;

    @Builder
    public TurnResultEvent(TurnResult turnResult, int finalCoordinateNumber,
                           List<Integer> diceResults,
                           int inning, boolean isTop,
                           int homeScore, int awayScore,
                           int balls, int strikes, int outs,
                           boolean firstBase, boolean secondBase, boolean thirdBase) {
        this.turnResult = turnResult;
        this.finalCoordinateNumber = finalCoordinateNumber;
        this.diceResults = diceResults;
        this.inning = inning;
        this.isTop = isTop;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.balls = balls;
        this.strikes = strikes;
        this.outs = outs;
        this.firstBase = firstBase;
        this.secondBase = secondBase;
        this.thirdBase = thirdBase;
    }
}
