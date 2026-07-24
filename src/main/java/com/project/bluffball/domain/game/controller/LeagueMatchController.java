package com.project.bluffball.domain.game.controller;

import com.project.bluffball.domain.game.dto.request.LeagueMatchQueueJoinRequest;
import com.project.bluffball.domain.game.dto.response.MatchJoinResponse;
import com.project.bluffball.global.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 리그 매칭 큐 REST 컨트롤러.
 *
 * <p>format × tier 단위로 매칭한다. 먼저 큐에 진입한 팀이 홈이다.
 * 대기 중 성사 알림은 {@code /topic/user/{userId}/match}로 수신한다.</p>
 */
@Tag(name = "League Match", description = "리그 매칭 큐 API")
@RestController
@RequestMapping("/api/v1/league-match")
@RequiredArgsConstructor
public class LeagueMatchController {

    /** JWT SecurityContext에서 userId 추출 */
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * 리그 매칭 큐에 진입한다.
     *
     * @param request format·tier
     * @return WAITING(202) 또는 MATCHED(200)
     */
    @Operation(
            summary = "리그 매칭 큐 진입",
            description = "같은 format×tier 상대와 매칭한다. "
                    + "출전 로스터·구종 사전 선택이 완료된 팀만 진입 가능하다. "
                    + "선진입 팀이 홈이다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매칭 성사 — status=MATCHED, matchSessionId 포함"),
            @ApiResponse(responseCode = "202", description = "매칭 대기 중 — status=WAITING"),
            @ApiResponse(responseCode = "400", description = "로스터·카드 미완성 또는 인원 불일치"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "409", description = "이미 큐에 등록된 유저")
    })
    @PostMapping("/queue/join")
    public ResponseEntity<MatchJoinResponse> joinQueue(
            @Valid @RequestBody LeagueMatchQueueJoinRequest request) {
        Long userId = authenticatedUserResolver.requireUserId();
        // TODO: LeagueMatchService.joinQueue(userId, request) → 200/202 분기
        return ResponseEntity.ok().build();
    }

    /**
     * 리그 매칭 큐를 취소한다.
     *
     * @return 본문 없음
     */
    @Operation(
            summary = "리그 매칭 큐 취소",
            description = "대기 중인 리그 매칭 큐에서 이탈한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "큐 취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "큐에 등록된 유저 없음")
    })
    @DeleteMapping("/queue/cancel")
    public ResponseEntity<Void> cancelQueue() {
        Long userId = authenticatedUserResolver.requireUserId();
        // TODO: leagueMatchService.cancelQueue(userId);
        return ResponseEntity.noContent().build();
    }
}
