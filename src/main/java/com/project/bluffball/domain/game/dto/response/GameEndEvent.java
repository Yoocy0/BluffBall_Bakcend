package com.project.bluffball.domain.game.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 경기 종료 WebSocket 송신 이벤트.
 * 경기가 최종 종료되면 양측 모두에게 전송된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameEndEvent {

    /** 최종 홈팀 점수 */
    private int homeScore;

    /** 최종 어웨이팀 점수 */
    private int awayScore;

    /** 승리자 유저 ID (동점 무승부 시 null) */
    private Long winnerUserId;

    @Builder
    public GameEndEvent(int homeScore, int awayScore, Long winnerUserId) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.winnerUserId = winnerUserId;
    }
}
