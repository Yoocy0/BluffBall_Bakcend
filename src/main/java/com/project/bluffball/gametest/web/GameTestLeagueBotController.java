package com.project.bluffball.gametest.web;

import com.project.bluffball.gametest.dto.EnqueueOpponentBotRequest;
import com.project.bluffball.gametest.dto.EnqueueOpponentBotResponse;
import com.project.bluffball.gametest.dto.FillCompactRosterRequest;
import com.project.bluffball.gametest.dto.FillCompactRosterResponse;
import com.project.bluffball.gametest.dto.FillRosterRequest;
import com.project.bluffball.gametest.dto.FillRosterResponse;
import com.project.bluffball.gametest.dto.StartBotAutoPlayRequest;
import com.project.bluffball.gametest.dto.StartBotAutoPlayResponse;
import com.project.bluffball.gametest.dto.TestSubstitutePitcherRequest;
import com.project.bluffball.gametest.dto.TestSubstitutePitcherResponse;
import com.project.bluffball.gametest.service.GameTestBotAutoPlayService;
import com.project.bluffball.gametest.service.GameTestLeagueBotService;
import com.project.bluffball.gametest.service.GameTestPitcherSubstituteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테스트용 리그 봇 API.
 */
@RestController
@RequestMapping("/game-test/api/bots")
@RequiredArgsConstructor
@Slf4j
public class GameTestLeagueBotController {

    private final GameTestLeagueBotService gameTestLeagueBotService;
    private final GameTestBotAutoPlayService gameTestBotAutoPlayService;
    private final GameTestPitcherSubstituteService gameTestPitcherSubstituteService;

    /**
     * Compact / Full 팀 로스터 빈자리를 봇으로 채운다.
     *
     * @param request 리더·팀·포맷·본인 역할
     * @return 채움 결과
     */
    @PostMapping("/league/fill-roster")
    public FillRosterResponse fillRoster(@Valid @RequestBody FillRosterRequest request) {
        log.info("[game-test] fill-roster leader={} teamId={} format={} myRole={}",
                request.leaderUserId(), request.teamId(), request.format(), request.myRole());
        return gameTestLeagueBotService.fillRoster(request);
    }

    /**
     * Compact 팀 로스터 빈자리를 봇으로 채운다 (하위 호환).
     *
     * @param request 리더·팀·본인 역할
     * @return 채움 결과
     */
    @PostMapping("/league/fill-compact-roster")
    public FillCompactRosterResponse fillCompactRoster(
            @Valid @RequestBody FillCompactRosterRequest request) {
        log.info("[game-test] fill-compact-roster leader={} teamId={} myRole={}",
                request.leaderUserId(), request.teamId(), request.myRole());
        return gameTestLeagueBotService.fillCompactRoster(request);
    }

    /**
     * 상대 봇 팀을 만들고 매칭 큐에 넣는다.
     *
     * @param request 포맷·티어(선택, 기본 Compact / 아마4)
     * @return 봇 팀·큐 상태
     */
    @PostMapping("/league/enqueue-opponent")
    public EnqueueOpponentBotResponse enqueueOpponent(
            @RequestBody(required = false) EnqueueOpponentBotRequest request) {
        EnqueueOpponentBotRequest body =
                request != null ? request : new EnqueueOpponentBotRequest(null, null);
        log.info("[game-test] enqueue-opponent format={} tier={}", body.format(), body.tier());
        return gameTestLeagueBotService.enqueueOpponentBotTeam(body);
    }

    /**
     * 봇 자동 플레이 루프를 시작한다.
     *
     * @param request 매치·봇 ID
     * @return 시작 결과
     */
    @PostMapping("/league/auto-play")
    public StartBotAutoPlayResponse startAutoPlay(
            @Valid @RequestBody StartBotAutoPlayRequest request) {
        log.info("[game-test] auto-play start match={} bots={}",
                request.matchSessionId(), request.botUserIds().size());
        return gameTestBotAutoPlayService.start(request);
    }

    /**
     * 봇 자동 플레이를 중지한다.
     *
     * @param matchSessionId 매치 세션
     */
    @PostMapping("/league/auto-play/{matchSessionId}/stop")
    public void stopAutoPlay(@PathVariable String matchSessionId) {
        log.info("[game-test] auto-play stop match={}", matchSessionId);
        gameTestBotAutoPlayService.stop(matchSessionId);
    }

    /**
     * 테스트용 투수 교체 (현재 등판 투수 권한으로 대행).
     *
     * @param request 매치·신임 투수
     * @return 교체 결과
     */
    @PostMapping("/league/substitute-pitcher")
    public TestSubstitutePitcherResponse substitutePitcher(
            @Valid @RequestBody TestSubstitutePitcherRequest request) {
        log.info("[game-test] substitute-pitcher match={} new={}",
                request.matchSessionId(), request.newPitcherUserId());
        return gameTestPitcherSubstituteService.substitute(request);
    }
}
