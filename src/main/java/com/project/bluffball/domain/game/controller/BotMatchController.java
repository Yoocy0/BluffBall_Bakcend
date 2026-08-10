package com.project.bluffball.domain.game.controller;

import com.project.bluffball.domain.game.dto.request.BotMatchStartRequest;
import com.project.bluffball.domain.game.dto.response.BotMatchStartResponse;
import com.project.bluffball.domain.game.service.BotMatchService;
import com.project.bluffball.global.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Showdown 봇 매치 REST 컨트롤러.
 *
 * <p>공개 PvP 큐를 거치지 않고 사람 vs 봇 연습 매치를 바로 생성한다.
 * 클라이언트는 응답의 {@code matchSessionId}로 WebSocket({@code /ws})에 연결한다.</p>
 *
 * <p>진입 경로:
 * <ul>
 *   <li>명시적 "봇과 플레이"</li>
 *   <li>매칭 대기 후 봇으로 전환 — {@code fromMatchmaking=true}</li>
 * </ul>
 * </p>
 */
@Tag(name = "Bot Match", description = "Showdown 봇 연습 매치 API (1v1)")
@RestController
@RequestMapping("/api/v1/match/bot")
@RequiredArgsConstructor
public class BotMatchController {

    /** 봇 매치 비즈니스 로직 */
    private final BotMatchService botMatchService;

    /** JWT SecurityContext에서 userId 추출 */
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * Showdown 봇 매치를 시작한다.
     *
     * @param request 난이도·매칭 전환 여부
     * @return matchSessionId·botUserId 등
     */
    @Operation(
            summary = "Showdown 봇 매치 시작",
            description = "사람 vs 봇 Showdown(1v1) 연습 매치를 생성한다. "
                    + "공개 매칭 큐를 사용하지 않으며 보상·래더는 없다. "
                    + "fromMatchmaking=true이면 대기 중인 Showdown 큐를 취소한 뒤 봇 매치를 시작한다. "
                    + "응답 matchSessionId로 /ws 연결 후 /topic/game/{matchSessionId} 등을 구독한다. "
                    + "매칭 성사 알림은 /topic/user/{userId}/match 로도 전송된다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "봇 매치 생성 성공"),
            @ApiResponse(responseCode = "400", description = "난이도 등 요청 값 오류"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료")
    })
    @PostMapping("/showdown/start")
    public ResponseEntity<BotMatchStartResponse> startShowdown(
            @Valid @RequestBody BotMatchStartRequest request) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(botMatchService.startShowdown(userId, request));
    }
}
