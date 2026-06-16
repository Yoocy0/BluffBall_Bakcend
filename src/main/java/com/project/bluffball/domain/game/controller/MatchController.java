package com.project.bluffball.domain.game.controller;

import com.project.bluffball.domain.game.dto.response.MatchCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 */
@Tag(name = "Match", description = "매칭 큐 진입/취소 API (싱글 모드 MVP)")
@RestController
@RequestMapping("/api/v1/match")
public class MatchController {

    /**
     * 싱글 모드 매칭 큐 진입.
     *
     * <p>요청 유저를 싱글 모드 매칭 큐에 등록한다.
     * 상대방이 큐에 있으면 즉시 매칭을 성사시키고 {@code matchSessionId}를 반환한다.
     * 상대가 없으면 큐에서 대기하며, 매칭 성사 시 WebSocket을 통해 별도 통보한다.</p>
     *
     * <p>클라이언트는 응답으로 받은 {@code matchSessionId}를 들고
     * {@code /ws} WebSocket에 연결하여 {@code /topic/game/{matchSessionId}}를 구독한다.</p>
     */
    @Operation(
            summary = "싱글 모드 매칭 큐 진입",
            description = "싱글 모드(1 vs 1 / 1이닝) 매칭 큐에 진입한다. " +
                    "매칭 성사 시 matchSessionId를 반환하며, 클라이언트는 이 값으로 WebSocket에 연결한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매칭 성사 — matchSessionId 반환"),
            @ApiResponse(responseCode = "202", description = "매칭 대기 중 — 큐 등록 완료"),
            @ApiResponse(responseCode = "409", description = "이미 큐에 등록된 유저")
    })
    @PostMapping("/queue/join")
    public ResponseEntity<MatchCreateResponse> joinQueue() {
        // TODO: MatchService.joinQueue(authenticatedUserId)
        return ResponseEntity.ok().build();
    }

    /**
     * 싱글 모드 매칭 큐 취소.
     *
     * <p>대기 중인 매칭 큐에서 요청 유저를 제거한다.
     * 이미 매칭이 성사된 경우에는 취소가 불가하며 에러를 반환한다.</p>
     */
    @Operation(
            summary = "싱글 모드 매칭 큐 취소",
            description = "대기 중인 매칭 큐에서 이탈한다. 이미 매칭이 성사된 경우 취소 불가."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "큐 취소 성공"),
            @ApiResponse(responseCode = "404", description = "큐에 등록된 유저 없음"),
            @ApiResponse(responseCode = "409", description = "이미 매칭이 성사되어 취소 불가")
    })
    @DeleteMapping("/queue/cancel")
    public ResponseEntity<Void> cancelQueue() {
        // TODO: MatchService.cancelQueue(authenticatedUserId)
        return ResponseEntity.noContent().build();
    }
}
