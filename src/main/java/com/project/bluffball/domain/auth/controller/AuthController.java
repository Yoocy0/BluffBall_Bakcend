package com.project.bluffball.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 소셜 OAuth 로그인 컨트롤러.
 *
 * <p>네이버/카카오/구글 소셜 로그인을 처리하고 JWT Access Token을 발급한다.
 * 클라이언트는 발급받은 토큰을 이후 모든 REST API 요청의
 * {@code Authorization: Bearer {token}} 헤더에 포함시킨다.</p>
 */
@Tag(name = "Auth", description = "소셜 OAuth 로그인 / 토큰 재발급 API")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /**
     * 소셜 로그인.
     *
     * <p>클라이언트가 소셜 플랫폼으로부터 받은 인가 코드(Authorization Code)를
     * 서버에 전달하면, 서버가 소셜 API를 호출하여 유저 정보를 검증하고
     * JWT Access Token과 Refresh Token을 발급한다.</p>
     *
     * <p>지원 provider:</p>
     * <ul>
     *   <li>{@code naver} — 네이버 로그인</li>
     *   <li>{@code kakao} — 카카오 로그인</li>
     *   <li>{@code google} — 구글 로그인</li>
     * </ul>
     *
     * <p>최초 로그인 시 자동으로 회원가입 처리된다.</p>
     */
    @Operation(
            summary = "소셜 로그인",
            description = "소셜 플랫폼의 인가 코드로 JWT Access Token과 Refresh Token을 발급한다. " +
                    "최초 로그인 시 자동 회원가입된다. provider: naver | kakao | google"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공 — Access/Refresh Token 반환"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 provider 또는 인가 코드"),
            @ApiResponse(responseCode = "503", description = "소셜 플랫폼 API 호출 실패")
    })
    @PostMapping("/login/{provider}")
    public ResponseEntity<?> login(
            @Parameter(description = "소셜 로그인 제공자 (naver | kakao | google)", example = "kakao")
            @PathVariable String provider) {
        // TODO: AuthService.login(provider, authorizationCode)
        return ResponseEntity.ok().build();
    }
}
