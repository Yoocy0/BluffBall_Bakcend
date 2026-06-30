package com.project.bluffball.domain.auth.controller;

import com.project.bluffball.domain.auth.dto.request.SocialLoginRequest;
import com.project.bluffball.domain.auth.dto.request.TokenRefreshRequest;
import com.project.bluffball.domain.auth.dto.response.LoginResponse;
import com.project.bluffball.domain.auth.dto.response.TokenRefreshResponse;
import com.project.bluffball.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 소셜 OAuth 로그인 컨트롤러.
 *
 * <p>카카오·구글 소셜 로그인 및 JWT 토큰 재발급을 처리한다.
 * 클라이언트는 발급받은 Access Token을 이후 모든 REST API 요청의
 * {@code Authorization: Bearer {token}} 헤더에 포함시킨다.</p>
 */
@Tag(name = "Auth", description = "소셜 OAuth 로그인 / 토큰 재발급 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 소셜 로그인.
     *
     * <p>클라이언트가 소셜 플랫폼으로부터 받은 인가 코드(Authorization Code)와
     * OAuth 요청에 사용한 redirect URI를 서버에 전달하면, 서버가 소셜 API를 호출하여
     * 유저 정보를 검증하고 JWT Access Token과 Refresh Token을 발급한다.</p>
     *
     * <p>redirect URI는 서버에 등록된 허용 주소 목록과 일치해야 한다.</p>
     *
     * <p>지원 provider:</p>
     * <ul>
     *   <li>{@code kakao} — 카카오 로그인</li>
     *   <li>{@code google} — 구글 로그인</li>
     * </ul>
     *
     * <p>최초 로그인 시 서버에서 닉네임을 자동 생성하여 회원가입 처리된다.</p>
     */
    @Operation(
            summary = "소셜 로그인",
            description = "소셜 플랫폼의 인가 코드와 redirect URI로 JWT Access Token과 Refresh Token을 발급한다. " +
                    "redirect URI는 허용 목록 검증을 통과해야 한다. " +
                    "최초 로그인 시 자동 회원가입된다. provider: kakao | google"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공 — Access/Refresh Token 반환"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 provider, redirect URI 또는 인가 코드"),
            @ApiResponse(responseCode = "403", description = "정지된 계정"),
            @ApiResponse(responseCode = "503", description = "소셜 플랫폼 API 호출 실패")
    })
    @PostMapping("/login/{provider}")
    public ResponseEntity<LoginResponse> login(
            @Parameter(description = "소셜 로그인 제공자 (kakao | google)", example = "kakao")
            @PathVariable String provider,
            @Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(authService.login(provider, request));
    }

    /**
     * Access Token 재발급.
     *
     * <p>만료된 Access Token 대신 유효한 Refresh Token을 제출하면
     * 새 Access Token과 Refresh Token을 발급한다.
     * Refresh Token Rotation 정책에 따라 기존 Refresh Token은 무효화된다.</p>
     */
    @Operation(
            summary = "Access Token 재발급",
            description = "유효한 Refresh Token으로 새 Access Token과 Refresh Token을 발급한다. " +
                    "기존 Refresh Token은 Rotation 정책에 따라 무효화된다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공 — 새 Access/Refresh Token 반환"),
            @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Refresh Token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(
            @Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
