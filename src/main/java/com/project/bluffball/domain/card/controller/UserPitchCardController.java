package com.project.bluffball.domain.card.controller;

import com.project.bluffball.domain.card.dto.request.EnhanceTimingRequest;
import com.project.bluffball.domain.card.dto.response.UserPitchCardResponse;
import com.project.bluffball.domain.card.service.UserPitchCardService;
import com.project.bluffball.global.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 유저 보유 구종 카드·강화 REST 컨트롤러.
 */
@Tag(name = "User Pitch Card", description = "유저 구종 카드 보유·강화 API")
@RestController
@RequestMapping("/api/v1/users/me/pitch-cards")
@RequiredArgsConstructor
public class UserPitchCardController {

    /** 보유·강화 서비스 */
    private final UserPitchCardService userPitchCardService;

    /** JWT userId 추출 */
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * 내 보유 구종 카드 목록을 조회한다.
     *
     * @return 보유 목록 (실효 스탯·코스트 포함)
     */
    @Operation(
            summary = "내 구종 카드 목록",
            description = "보유 구종과 강화 상태·실효 타이밍/변화량·핸드 코스트를 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료")
    })
    @GetMapping
    public ResponseEntity<List<UserPitchCardResponse>> getMyCards() {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(userPitchCardService.getMyCards(userId));
    }

    /**
     * 마스터 구종 1장을 획득한다.
     *
     * @param cardId 마스터 구종 ID
     * @return 획득한 카드
     */
    @Operation(
            summary = "구종 카드 획득",
            description = "마스터 구종 카탈로그의 카드를 미강화 상태로 획득한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "획득 성공"),
            @ApiResponse(responseCode = "404", description = "마스터 구종 없음"),
            @ApiResponse(responseCode = "409", description = "이미 보유")
    })
    @PostMapping("/{cardId}/acquire")
    public ResponseEntity<UserPitchCardResponse> acquire(
            @Parameter(description = "마스터 구종 카드 ID")
            @PathVariable Long cardId) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(userPitchCardService.acquire(userId, cardId));
    }

    /**
     * 미보유 마스터 구종을 모두 획득한다.
     *
     * @return 전체 보유 목록
     */
    @Operation(
            summary = "미보유 구종 일괄 획득",
            description = "아직 없는 마스터 구종을 모두 미강화로 획득한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "획득 완료")
    })
    @PostMapping("/acquire-all")
    public ResponseEntity<List<UserPitchCardResponse>> acquireAllMissing() {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(userPitchCardService.acquireAllMissing(userId));
    }

    /**
     * 변화량 강화를 적용한다 (+1, 1회).
     *
     * @param cardId 마스터 구종 ID
     * @return 강화 후 카드
     */
    @Operation(
            summary = "변화량 강화",
            description = "변화량 +1. 카드당 1회. 핸드 코스트 +1.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "강화 성공"),
            @ApiResponse(responseCode = "404", description = "미보유"),
            @ApiResponse(responseCode = "409", description = "이미 강화됨")
    })
    @PostMapping("/{cardId}/enhance/change-amount")
    public ResponseEntity<UserPitchCardResponse> enhanceChangeAmount(@PathVariable Long cardId) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(userPitchCardService.enhanceChangeAmount(userId, cardId));
    }

    /**
     * 타이밍 강화를 적용한다 (FASTER/SLOWER, 1회).
     *
     * @param cardId 마스터 구종 ID
     * @param request 강화 방향
     * @return 강화 후 카드
     */
    @Operation(
            summary = "타이밍 강화",
            description = "FASTER(−1) 또는 SLOWER(+1). 카드당 1회. 극점 경계는 거부. 핸드 코스트 +1.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "강화 성공"),
            @ApiResponse(responseCode = "400", description = "방향 오류 또는 경계 초과"),
            @ApiResponse(responseCode = "404", description = "미보유"),
            @ApiResponse(responseCode = "409", description = "이미 강화됨")
    })
    @PostMapping("/{cardId}/enhance/timing")
    public ResponseEntity<UserPitchCardResponse> enhanceTiming(
            @PathVariable Long cardId,
            @Valid @RequestBody EnhanceTimingRequest request) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(userPitchCardService.enhanceTiming(userId, cardId, request));
    }
}
