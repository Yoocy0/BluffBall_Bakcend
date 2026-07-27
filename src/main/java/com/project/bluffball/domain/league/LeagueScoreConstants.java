package com.project.bluffball.domain.league;

/**
 * 리그 점수·매치 보상 상수.
 *
 * <p>밸런스 조정 시 이 값만 변경한다.</p>
 */
public final class LeagueScoreConstants {

    /** 경기 승패 가감점 (승 +MATCH_POINTS, 패 -MATCH_POINTS) + 득실 */
    public static final int MATCH_POINTS = 5;

    /**
     * 주기 배치(Compact 주간 / Full 월간) 시 차감하는 점수.
     * 약 2~5경기 분량의 승점 스케일.
     */
    public static final int PERIODIC_RATING_DECAY = 30;

    /** 티어 인덱스(1~7)당 승리 재화 */
    public static final long MATCH_WIN_REWARD_PER_TIER = 100L;

    /** 티어 인덱스(1~7)당 패배 재화 */
    public static final long MATCH_LOSS_REWARD_PER_TIER = 30L;

    private LeagueScoreConstants() {
    }
}
