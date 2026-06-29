package com.project.bluffball.gametest.web;

import com.project.bluffball.domain.game.dto.request.MulliganRequest;
import com.project.bluffball.domain.game.dto.request.PitcherCardSelectRequest;
import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.gametest.dto.TestMatchCreateResponse;
import com.project.bluffball.gametest.dto.TestMatchSetupNumbersResponse;
import com.project.bluffball.gametest.dto.TestPitchHandResponse;
import com.project.bluffball.gametest.dto.TestPitcherSelectResponse;
import com.project.bluffball.gametest.dto.TestPreparePitchResponse;
import com.project.bluffball.gametest.service.GameTestMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게임 로직 수동 테스트용 REST API.
 */
@RestController
@RequestMapping("/game-test/api")
@RequiredArgsConstructor
@Slf4j
public class GameTestMatchController {

    private final GameTestMatchService gameTestMatchService;

    @PostMapping("/match")
    public TestMatchCreateResponse createMatch() {
        return gameTestMatchService.createSingleTestMatch();
    }

    @GetMapping("/match/{matchSessionId}/setup-numbers")
    public TestMatchSetupNumbersResponse getSetupNumbers(@PathVariable String matchSessionId) {
        return gameTestMatchService.getSetupNumbers(matchSessionId);
    }

    @PostMapping("/match/{matchSessionId}/setup-numbers")
    public TestMatchSetupNumbersResponse submitSetupNumbers(
            @PathVariable String matchSessionId,
            @RequestBody SetupNumberRequest request) {
        log.info("[game-test] setup-numbers 제출 matchSessionId={} out={} dp={} triple={} hr={}",
                matchSessionId, request.outNumList(), request.dpNumList(),
                request.tripleNumList(), request.hrNumList());
        return gameTestMatchService.submitSetupNumbers(matchSessionId, request);
    }

    /** 멀리건 화면 — setup 완료 + 구종 3장 드로우 */
    @PostMapping("/match/{matchSessionId}/prepare-draw")
    public TestPitchHandResponse prepareForDraw(@PathVariable String matchSessionId) {
        log.info("[game-test] prepare-draw matchSessionId={}", matchSessionId);
        return gameTestMatchService.prepareForDraw(matchSessionId);
    }

    @GetMapping("/match/{matchSessionId}/pitch-hand")
    public TestPitchHandResponse getPitchHand(@PathVariable String matchSessionId) {
        return gameTestMatchService.getPitchHand(matchSessionId);
    }

    /** 멀리건 교체 */
    @PostMapping("/match/{matchSessionId}/mulligan/swap")
    public TestPitchHandResponse swapMulligan(
            @PathVariable String matchSessionId,
            @RequestBody MulliganRequest request) {
        log.info("[game-test] mulligan/swap matchSessionId={} swap={}", matchSessionId, request.cardIdsToSwap());
        return gameTestMatchService.swapMulligan(matchSessionId, request);
    }

    /** 멀리건 확정 (교체 없음) */
    @PostMapping("/match/{matchSessionId}/mulligan/confirm")
    public TestPitchHandResponse confirmMulligan(@PathVariable String matchSessionId) {
        log.info("[game-test] mulligan/confirm matchSessionId={}", matchSessionId);
        return gameTestMatchService.confirmMulligan(matchSessionId);
    }

    /** 투수 선택 화면 — 멀리건 완료 후 패·좌표 로드 */
    @PostMapping("/match/{matchSessionId}/prepare-pitch")
    public TestPreparePitchResponse prepareForPitcher(@PathVariable String matchSessionId) {
        log.info("[game-test] prepare-pitch matchSessionId={}", matchSessionId);
        return gameTestMatchService.prepareForPitcher(matchSessionId);
    }

    @PostMapping("/match/{matchSessionId}/pitcher/select-card")
    public TestPitcherSelectResponse selectPitcherCard(
            @PathVariable String matchSessionId,
            @RequestBody PitcherCardSelectRequest request) {
        log.info("[game-test] pitcher/select-card matchSessionId={} pitch={} coord={}",
                matchSessionId, request.pitchCardId(), request.coordinateCardId());
        return gameTestMatchService.selectPitcherCard(matchSessionId, request);
    }
}
