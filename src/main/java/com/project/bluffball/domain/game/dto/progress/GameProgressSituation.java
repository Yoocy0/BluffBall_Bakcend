package com.project.bluffball.domain.game.dto.progress;

/**
 * 경기 진행 계산 입력용 스냅샷 (순수 값만).
 *
 * <p>초=어웨이 공격, 말=홈 공격으로 점수를 반영한다.</p>
 */
public record GameProgressSituation(
        int totalInnings,
        int currentInning,
        boolean isTop,
        int homeScore,
        int awayScore,
        int balls,
        int strikes,
        int outs,
        boolean firstBase,
        boolean secondBase,
        boolean thirdBase
) {
    public boolean hasRunnersOnBase() {
        return firstBase || secondBase || thirdBase;
    }
}
