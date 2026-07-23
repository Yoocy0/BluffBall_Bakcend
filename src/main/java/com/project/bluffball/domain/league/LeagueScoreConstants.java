package com.project.bluffball.domain.league;

/**
 * 리그 순위 점수 정책 상수.
 *
 * <p>밸런스 조정 시 이 값만 변경한다. DB 컬럼이 아니다.</p>
 */
public final class LeagueScoreConstants {

    /** 리그 시작 시 모든 팀의 기본 점수 */
    public static final int BASE_SCORE = 1000;

    /**
     * 경기 결과 가감점.
     * 승리는 {@code +MATCH_POINTS}, 패배는 {@code -MATCH_POINTS}.
     */
    public static final int MATCH_POINTS = 5;

    private LeagueScoreConstants() {
    }
}
