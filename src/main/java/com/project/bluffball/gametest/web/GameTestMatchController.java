package com.project.bluffball.gametest.web;

import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.gametest.dto.TestMatchCreateResponse;
import com.project.bluffball.gametest.dto.TestMatchSetupNumbersResponse;
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

    /**
     * 싱글 모드 테스트 매치 1건을 Redis에 생성한다.
     *
     * <p>WebSocket 컨트롤러가 {@code userId=1L}로 고정된 상태이므로
     * 투수·타자 모두 userId 1로 seed한다.</p>
     */
    @PostMapping("/match")
    public TestMatchCreateResponse createMatch() {
        return gameTestMatchService.createSingleTestMatch();
    }

    /** Redis에 저장된 블러핑 숫자 확인 (테스트 전용) */
    @GetMapping("/match/{matchSessionId}/setup-numbers")
    public TestMatchSetupNumbersResponse getSetupNumbers(@PathVariable String matchSessionId) {
        return gameTestMatchService.getSetupNumbers(matchSessionId);
    }

    /**
     * 테스트 화면용 setup-numbers 제출 — WebSocket과 동일하게 {@link com.project.bluffball.domain.game.service.GamePrepService} 호출.
     */
    @PostMapping("/match/{matchSessionId}/setup-numbers")
    public TestMatchSetupNumbersResponse submitSetupNumbers(
            @PathVariable String matchSessionId,
            @RequestBody SetupNumberRequest request) {
        log.info("[game-test] setup-numbers 제출 matchSessionId={} out={} dp={} triple={} hr={}",
                matchSessionId, request.outNumList(), request.dpNumList(),
                request.tripleNumList(), request.hrNumList());
        return gameTestMatchService.submitSetupNumbers(matchSessionId, request);
    }
}
