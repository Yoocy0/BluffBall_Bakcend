package com.project.bluffball.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 유저 프로필 컨트롤러.
 *
 * <p>인증된 유저의 프로필 및 전적 정보를 조회하는 API를 제공한다.
 * 메인 로비 화면 진입 시 최신 상태를 동기화하기 위해 호출된다.</p>
 */
@Tag(name = "User", description = "유저 프로필 조회 API")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    /**
     * 내 프로필 조회.
     *
     * <p>JWT 토큰으로 인증된 유저 본인의 프로필 정보를 반환한다.
     * 메인 로비 화면 진입 시 자동 호출되어 닉네임, 전적, 에너지 잔여량 등
     * 최신 상태를 동기화하는 데 사용된다.</p>
     */
    @Operation(
            summary = "내 프로필 조회",
            description = "JWT 토큰으로 인증된 본인의 프로필(닉네임, 전적, 에너지)을 반환한다. " +
                    "메인 로비 화면 진입 시 자동 호출된다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료")
    })
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        // TODO: UserService.getMyProfile(authenticatedUserId)
        return ResponseEntity.ok().build();
    }
}
