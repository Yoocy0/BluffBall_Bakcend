package com.project.bluffball.domain.card.controller;

import com.project.bluffball.domain.card.EnhancementConstants;
import com.project.bluffball.domain.card.dto.request.ApplyEnhancementRequest;
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
 * 유저 구종 인스턴스·강화 REST 컨트롤러.
 */
@Tag(name = "User Pitch Card", description = "유저 구종 카드 인스턴스 보유·강화 API")
@RestController
@RequestMapping("/api/v1/users/me/pitch-cards")
@RequiredArgsConstructor
public class UserPitchCardController {

    private final UserPitchCardService userPitchCardService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Operation(summary = "내 구종 인스턴스 목록", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "성공")})
    @GetMapping
    public ResponseEntity<List<UserPitchCardResponse>> getMyCards() {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(userPitchCardService.getMyCards(userId));
    }

    @Operation(
            summary = "구종 기본본 획득",
            description = "마스터 구종 기본본 인스턴스를 새로 만든다 (중복 보유 가능).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{cardId}/acquire")
    public ResponseEntity<UserPitchCardResponse> acquire(
            @Parameter(description = "마스터 구종 ID") @PathVariable Long cardId) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(userPitchCardService.acquire(userId, cardId));
    }

    @Operation(summary = "미보유 마스터 일괄 기본본 획득", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/acquire-all")
    public ResponseEntity<List<UserPitchCardResponse>> acquireAllMissing() {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(userPitchCardService.acquireAllMissing(userId));
    }

    @Operation(
            summary = "강화 카드로 구종 인스턴스 강화",
            description = "path의 ID는 userPitchCardId(인스턴스). 강화 카드 1장 소모.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{userPitchCardId}/enhance")
    public ResponseEntity<UserPitchCardResponse> applyEnhancement(
            @PathVariable Long userPitchCardId,
            @Valid @RequestBody ApplyEnhancementRequest request) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(
                userPitchCardService.applyEnhancement(userId, userPitchCardId, request));
    }

    @Operation(
            summary = "변화량 강화 되돌리기",
            description = "재화 " + EnhancementConstants.REVERT_COST + " 소모. path는 userPitchCardId.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{userPitchCardId}/enhance/revert/change-amount")
    public ResponseEntity<UserPitchCardResponse> revertChangeAmount(
            @PathVariable Long userPitchCardId) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(userPitchCardService.revertChangeAmount(userId, userPitchCardId));
    }

    @Operation(
            summary = "타이밍 강화 되돌리기",
            description = "재화 " + EnhancementConstants.REVERT_COST + " 소모. path는 userPitchCardId.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{userPitchCardId}/enhance/revert/timing")
    public ResponseEntity<UserPitchCardResponse> revertTiming(@PathVariable Long userPitchCardId) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(userPitchCardService.revertTiming(userId, userPitchCardId));
    }
}
