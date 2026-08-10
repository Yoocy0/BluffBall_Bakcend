package com.project.bluffball.domain.store.controller;

import com.project.bluffball.domain.store.dto.request.GooglePlayConfirmRequest;
import com.project.bluffball.domain.store.dto.request.StoreEnhancementPurchaseRequest;
import com.project.bluffball.domain.store.dto.request.StorePitchPurchaseRequest;
import com.project.bluffball.domain.store.dto.response.CurrencyProductListResponse;
import com.project.bluffball.domain.store.dto.response.GooglePlayConfirmResponse;
import com.project.bluffball.domain.store.dto.response.StoreCatalogResponse;
import com.project.bluffball.domain.store.dto.response.StoreEnhancementPurchaseResponse;
import com.project.bluffball.domain.store.dto.response.StorePitchPurchaseResponse;
import com.project.bluffball.domain.store.service.StoreService;
import com.project.bluffball.global.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상시 상점·Google Play 재화 충전 REST 컨트롤러.
 */
@Tag(name = "Store", description = "상시 구종·강화 카드 상점·Google Play 재화 충전 API")
@RestController
@RequestMapping("/api/v1/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Operation(
            summary = "상점 카탈로그",
            description = "구종 기본본(미강화 인스턴스 없을 때만 구매 가능)과 강화 카드(상시·수량 누적) 목록.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/catalog")
    public ResponseEntity<StoreCatalogResponse> getCatalog() {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(storeService.getCatalog(userId));
    }

    @Operation(
            summary = "구종 기본본 구매",
            description = "해당 마스터의 미강화 인스턴스가 없을 때만 구매. 새 userPitchCardId 인스턴스 생성.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/pitch/purchase")
    public ResponseEntity<StorePitchPurchaseResponse> purchasePitch(
            @Valid @RequestBody StorePitchPurchaseRequest request) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(storeService.purchasePitch(userId, request));
    }

    @Operation(
            summary = "강화 카드 구매",
            description = "강화 카드 1장 구매 (수량 누적).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/enhancement/purchase")
    public ResponseEntity<StoreEnhancementPurchaseResponse> purchaseEnhancement(
            @Valid @RequestBody StoreEnhancementPurchaseRequest request) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(storeService.purchaseEnhancement(userId, request));
    }

    @Operation(summary = "재화 상품 목록", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/currency/products")
    public ResponseEntity<CurrencyProductListResponse> getCurrencyProducts() {
        authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(storeService.getCurrencyProducts());
    }

    @Operation(summary = "Google Play 결제 확인", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "지급 성공"),
            @ApiResponse(responseCode = "409", description = "이미 처리된 결제")
    })
    @PostMapping("/currency/google/confirm")
    public ResponseEntity<GooglePlayConfirmResponse> confirmGooglePlay(
            @Valid @RequestBody GooglePlayConfirmRequest request) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(storeService.confirmGooglePlayPurchase(userId, request));
    }
}
