package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 리그 매칭 준비·성사 규칙 검증 (usecase/validator 계층).
 */
@Component
public class LeagueMatchValidator {

    /**
     * 로스터·선발 투수·구종 사전 선택이 완료됐는지 검증한다.
     *
     * @param lineupExists 로스터 존재 여부
     * @param hasStartingPitcher 선발 투수 지정 여부
     * @param pitchCardsComplete 구종 사전 선택 완료 여부
     * @throws BadRequestException 미완료 시
     */
    public void validateReady(
            boolean lineupExists,
            boolean hasStartingPitcher,
            boolean pitchCardsComplete) {
        if (!lineupExists || !hasStartingPitcher || !pitchCardsComplete) {
            throw new BadRequestException(
                    ErrorCode.LEAGUE_MATCH_NOT_READY,
                    "lineupExists=" + lineupExists
                            + ", hasStartingPitcher=" + hasStartingPitcher
                            + ", pitchComplete=" + pitchCardsComplete);
        }
    }

    /**
     * 동일 팀끼리 매칭되지 않도록 검증한다.
     *
     * @param homeTeamId 홈 팀 ID
     * @param awayTeamId 어웨이 팀 ID
     * @throws ConflictException 같은 팀이면
     */
    public void validateDifferentTeams(Long homeTeamId, Long awayTeamId) {
        if (homeTeamId != null && homeTeamId.equals(awayTeamId)) {
            throw new ConflictException(ErrorCode.LEAGUE_MATCH_SAME_TEAM);
        }
    }
}
