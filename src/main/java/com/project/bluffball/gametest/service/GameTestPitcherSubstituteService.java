package com.project.bluffball.gametest.service;

import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.dto.request.LeagueSubstitutePitcherRequest;
import com.project.bluffball.domain.game.service.GamePrepService;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.gametest.dto.TestSubstitutePitcherRequest;
import com.project.bluffball.gametest.dto.TestSubstitutePitcherResponse;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 테스트용 투수 교체 — 현재 등판 투수 권한으로 서버에서 교체를 대행한다.
 *
 * <p>본 권한(리더/투수) 검증은 이후 본 게임 API에서 다룬다.
 * 테스트 UI는 사람이 타자여도 수비 중 교체를 검증할 수 있게 한다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameTestPitcherSubstituteService {

    private final GamePrepService gamePrepService;
    private final MatchInfoReader matchInfoReader;
    private final GameModeRule gameModeRule;

    /**
     * 현재 투수 명의로 교체를 수행한다.
     *
     * @param request 매치·신임 투수
     * @return 교체 결과
     */
    public TestSubstitutePitcherResponse substitute(TestSubstitutePitcherRequest request) {
        String matchSessionId = request.matchSessionId();
        Long currentPitcher = matchInfoReader.getPitcherUserId(matchSessionId);
        if (currentPitcher == null) {
            throw new BadRequestException(ErrorCode.GAME_NOT_PITCHER, "no active pitcher");
        }

        log.info("[game-test] substitute-pitcher match={} asPitcher={} -> {}",
                matchSessionId, currentPitcher, request.newPitcherUserId());

        gamePrepService.substituteLeaguePitcher(
                matchSessionId,
                currentPitcher,
                new LeagueSubstitutePitcherRequest(request.newPitcherUserId()));

        int max = gameModeRule.getMaxPitcherSubstitutions(matchInfoReader.getGameMode(matchSessionId));
        int used = matchInfoReader.getPitcherSubstitutionCount(matchSessionId);
        Long newPitcher = matchInfoReader.getPitcherUserId(matchSessionId);

        return new TestSubstitutePitcherResponse(
                matchSessionId,
                newPitcher,
                currentPitcher,
                used,
                max);
    }
}
