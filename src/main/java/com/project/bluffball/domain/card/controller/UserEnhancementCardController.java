package com.project.bluffball.domain.card.controller;

import com.project.bluffball.domain.card.dto.response.UserEnhancementCardResponse;
import com.project.bluffball.domain.card.service.EnhancementCardService;
import com.project.bluffball.global.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 유저 강화 카드 보유 REST 컨트롤러.
 */
@Tag(name = "User Enhancement Card", description = "유저 강화 카드 보유 API")
@RestController
@RequestMapping("/api/v1/users/me/enhancement-cards")
@RequiredArgsConstructor
public class UserEnhancementCardController {

    /** 강화 카드 서비스 */
    private final EnhancementCardService enhancementCardService;

    /** JWT userId 추출 */
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * 내 강화 카드 보유 목록을 조회한다.
     *
     * @return 마스터 3종 + 수량 (미보유 0)
     */
    @Operation(
            summary = "내 강화 카드 목록",
            description = "변화량/타이밍 가속/감속 강화 카드 보유 수량을 반환한다. 미보유는 quantity=0.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping
    public ResponseEntity<List<UserEnhancementCardResponse>> getMyCards() {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(enhancementCardService.getMyInventory(userId));
    }

    /**
     * 강화 카드를 획득한다 (상점 연동 전 임시/테스트용).
     *
     * @param cardId 마스터 ID
     * @param amount 수량 (기본 1)
     * @return 해당 카드 인벤 항목
     */
    @Operation(
            summary = "강화 카드 획득",
            description = "강화 카드를 수량만큼 획득한다. 상점 구매 API 연동 전 테스트·지급용.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "획득 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "마스터 없음")
    })
    @PostMapping("/{cardId}/acquire")
    public ResponseEntity<UserEnhancementCardResponse> acquire(
            @Parameter(description = "강화 카드 마스터 ID")
            @PathVariable Long cardId,
            @Parameter(description = "획득 수량")
            @RequestParam(defaultValue = "1") int amount) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(enhancementCardService.grant(userId, cardId, amount));
    }
}
