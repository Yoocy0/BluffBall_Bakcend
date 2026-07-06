package com.project.bluffball.domain.game.controller;

import com.project.bluffball.domain.game.dto.response.MatchJoinResponse;
import com.project.bluffball.domain.game.service.MatchService;
import com.project.bluffball.global.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 싱글 모드 매칭 큐 REST 컨트롤러.
 *
 * <p>유저가 싱글 모드 매칭 큐에 진입/취소하는 기능을 담당한다.
 * 매칭이 성사되면 {@code matchSessionId}를 반환하며,
 * 클라이언트는 이 ID를 사용하여 WebSocket({@code /ws})에 연결한다.</p>
 *
 * <p>대기 중 매칭 성사 알림은 {@code /topic/user/{userId}/match} 개인 토픽으로 수신한다.
 * CONNECT 시 JWT를 native header로 전달해야 하며, 구독 경로의 userId는 토큰 subject와 일치해야 한다.</p>
 */
@Tag(name = "Match", description = "매칭 큐 진입/취소 API (싱글 모드 MVP)")
@RestController
@RequestMapping("/api/v1/match")
@RequiredArgsConstructor
public class MatchController {

    /** 싱글 모드 매칭 큐 진입·취소 비즈니스 로직 */
    private final MatchService matchService;

    /** JWT SecurityContext에서 userId 추출 */
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * 싱글 모드 매칭 큐 진입.
     *
     * <p>요청 유저를 싱글 모드 매칭 큐에 등록한다.
     * 상대방이 큐에 있으면 즉시 매칭을 성사시키고 {@code matchSessionId}를 반환한다.
     * 상대가 없으면 큐에서 대기하며, 매칭 성사 시 {@code /topic/user/{userId}/match}로 통보된다.</p>
     *
     * <p>선매칭 유저(먼저 큐에 있던 유저)가 투수, 후매칭 유저가 타자로 시작한다.</p>
     */
    @Operation(
            summary = "싱글 모드 매칭 큐 진입",
            description = "싱글 모드(1 vs 1 / 1이닝) 매칭 큐에 진입한다. " +
                    "즉시 매칭 시 status=MATCHED와 matchSessionId를 반환하고, " +
                    "대기 시 status=WAITING을 반환한다. " +
                    "대기 중 매칭 성사는 /topic/user/{userId}/match WebSocket으로 수신한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매칭 성사 — status=MATCHED, matchSessionId 포함"),
            @ApiResponse(responseCode = "202", description = "매칭 대기 중 — status=WAITING"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "409", description = "이미 큐에 등록된 유저")
    })
    @PostMapping("/queue/join")
    public ResponseEntity<MatchJoinResponse> joinQueue() {
        // JWT에서 인증된 요청 유저 ID 추출
        Long userId = authenticatedUserResolver.requireUserId();
        MatchJoinResponse response = matchService.joinQueue(userId);
        // WAITING → 202 Accepted / MATCHED → 200 OK
        return response.isWaiting()
                ? ResponseEntity.status(HttpStatus.ACCEPTED).body(response)
                : ResponseEntity.ok(response);
    }   

    /**
     * 싱글 모드 매칭 큐 취소.
     *
     * <p>대기 중인 매칭 큐에서 요청 유저를 제거한다.
     * MATCHED 잔여 Entry(경기 종료 후 재매칭 등)도 정리할 수 있다.</p>
     */
    @Operation(
            summary = "싱글 모드 매칭 큐 취소",
            description = "대기 중인 매칭 큐에서 이탈하거나, MATCHED 잔여 Entry를 정리한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "큐 취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "큐에 등록된 유저 없음")
    })
    @DeleteMapping("/queue/cancel")
    public ResponseEntity<Void> cancelQueue() {
        // JWT에서 인증된 요청 유저 ID 추출
        Long userId = authenticatedUserResolver.requireUserId();
        matchService.cancelQueue(userId);
        return ResponseEntity.noContent().build();
    }
}
