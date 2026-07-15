package com.project.bluffball.domain.game.controller;

import com.project.bluffball.domain.game.dto.response.GameSessionStateResponse;
import com.project.bluffball.domain.game.service.GameForfeitService;
import com.project.bluffball.domain.game.service.GameSessionService;
import com.project.bluffball.domain.game.service.usecase.reader.GameProgressReader;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.validator.GameProgressValidator;
import com.project.bluffball.domain.game.service.usecase.validator.MatchParticipantValidator;
import com.project.bluffball.global.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인게임 세션 조회 REST API.
 */
@Tag(name = "Game", description = "인게임 세션 상태 조회 API")
@RestController
@RequestMapping("/api/v1/game")
@RequiredArgsConstructor
public class GameController {

    private final GameSessionService gameSessionService;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final MatchInfoReader matchInfoReader;
    private final MatchParticipantValidator matchParticipantValidator;
    private final GameProgressReader gameProgressReader;
    private final GameProgressValidator gameProgressValidator;
    private final GameForfeitService gameForfeitService;

    /**
     * 모바일 재접속용 인게임 세션 스냅샷.
     *
     * <p>로컬에 저장된 {@code matchSessionId}와 JWT로 호출한다.
     * 응답의 {@code phase}로 화면을 복원하고, 이후 WebSocket을 재연결한다.</p>
     */
    @Operation(
            summary = "인게임 세션 상태 조회",
            description = "재접속 시 matchSessionId 기준으로 phase, 패, 스코어보드, 턴 상태를 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "세션 상태 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "매치 참가자 아님"),
            @ApiResponse(responseCode = "404", description = "매치 또는 게임 상태 없음")
    })
    @GetMapping("/{matchSessionId}/state")
    public GameSessionStateResponse getSessionState(@PathVariable String matchSessionId) {
        Long userId = authenticatedUserResolver.requireUserId();
        return gameSessionService.getSessionState(matchSessionId, userId);
    }

    /**
     * 인게임 자진 포기 — 몰수패(0:10) 처리 후 경기 종료 이벤트를 발행한다.
     */
    @Operation(
            summary = "자진 몰수패",
            description = "뒤로가기 등으로 경기를 고의 포기할 때 호출한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "몰수패 처리 완료"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "매치 참가자 아님"),
            @ApiResponse(responseCode = "400", description = "이미 종료된 경기")
    })
    @PostMapping("/{matchSessionId}/forfeit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void voluntaryForfeit(@PathVariable String matchSessionId) {
        Long userId = authenticatedUserResolver.requireUserId();
        matchParticipantValidator.validateParticipant(
                matchInfoReader.isParticipant(matchSessionId, userId));
        gameProgressValidator.validateGameActive(
                gameProgressReader.isInitialized(matchSessionId),
                gameProgressReader.isGameOver(matchSessionId));
        gameForfeitService.applyDisconnectForfeit(matchSessionId, userId);
    }
}
