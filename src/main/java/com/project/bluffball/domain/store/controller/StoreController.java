package com.project.bluffball.domain.store.controller;

import com.project.bluffball.domain.store.dto.request.GooglePlayConfirmRequest;
import com.project.bluffball.domain.store.dto.request.StorePitchPurchaseRequest;
import com.project.bluffball.domain.store.dto.response.CurrencyProductListResponse;
import com.project.bluffball.domain.store.dto.response.DailyStoreResponse;
import com.project.bluffball.domain.store.dto.response.GooglePlayConfirmResponse;
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
 * 상점·Google Play 재화 충전 REST 컨트롤러.
 */
@Tag(name = "Store", description = "일일 구종 상점·Google Play 재화 충전 API")
@RestController
@RequestMapping("/api/v1/store")
@RequiredArgsConstructor
public class StoreController {

    /** 상점 서비스 */
    private final StoreService storeService;

    /** JWT userId 추출 */
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * 오늘 일일 구종 상점을 조회한다.
     *
     * @return 오퍼 목록·잔액
     */
    @Operation(
            summary = "일일 상점 조회",
            description = "유저·날짜(KST) 시드로 매일 갱신되는 구종 3칸과 가격·보유/구매 여부를 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/daily")
    public ResponseEntity<DailyStoreResponse> getDailyStore() {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(storeService.getDailyStore(userId));
    }

    /**
     * 오늘 오퍼 구종을 재화로 구매한다.
     *
     * @param request 구매 요청
     * @return 구매 결과
     */
    @Operation(
            summary = "일일 상점 구종 구매",
            description = "오늘 오퍼에 포함된 미보유 구종을 재화로 구매한다. 잔액·오퍼·중복 구매를 서버에서 검증한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "구매 성공"),
            @ApiResponse(responseCode = "400", description = "오퍼 아님·재화 부족"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "409", description = "이미 보유·오늘 이미 구매")
    })
    @PostMapping("/daily/purchase")
    public ResponseEntity<StorePitchPurchaseResponse> purchasePitch(
            @Valid @RequestBody StorePitchPurchaseRequest request) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(storeService.purchasePitch(userId, request));
    }

    /**
     * Google Play 재화 상품 목록을 조회한다.
     *
     * @return 상품 목록
     */
    @Operation(
            summary = "재화 상품 목록",
            description = "Play Console productId(SKU)와 지급 재화량을 반환한다. 클라이언트 BillingClient와 동일 SKU를 사용한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/currency/products")
    public ResponseEntity<CurrencyProductListResponse> getCurrencyProducts() {
        authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(storeService.getCurrencyProducts());
    }

    /**
     * Google Play 결제를 확인하고 재화를 지급한다.
     *
     * @param request 결제 확인
     * @return 지급 결과
     */
    @Operation(
            summary = "Google Play 결제 확인",
            description = "클라이언트가 받은 purchaseToken을 서버에서 검증(MOCK/LIVE)한 뒤 재화를 지급한다. "
                    + "동일 토큰은 멱등 처리된다. LIVE는 Play Developer API + 서비스 계정 필요.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "지급 성공"),
            @ApiResponse(responseCode = "400", description = "상품/결제 무효"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "409", description = "이미 처리된 결제"),
            @ApiResponse(responseCode = "503", description = "Play API 설정 오류")
    })
    @PostMapping("/currency/google/confirm")
    public ResponseEntity<GooglePlayConfirmResponse> confirmGooglePlay(
            @Valid @RequestBody GooglePlayConfirmRequest request) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(storeService.confirmGooglePlayPurchase(userId, request));
    }
}
