package com.project.bluffball.gametest.web;

import com.project.bluffball.domain.auth.dto.response.LoginResponse;
import com.project.bluffball.domain.auth.infrastructure.jwt.JwtTokenProvider;
import com.project.bluffball.domain.user.enums.UserRole;
import com.project.bluffball.gametest.dto.GameTestDevLoginRequest;
import com.project.bluffball.gametest.dto.GameTestWsTokenResponse;
import com.project.bluffball.gametest.service.GameTestDevLoginService;
import com.project.bluffball.gametest.service.GameTestMatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게임 테스트 화면용 인증 보조 API.
 *
 * <p>OAuth 없이 WebSocket CONNECT JWT가 필요할 때 테스트 유저 토큰을 발급한다.
 * {@code /game-test/**} 경로 전용이며, 운영 본번 플로우에는 사용하지 않는다.</p>
 */
@Tag(name = "Game Test Auth", description = "게임 테스트용 인증 보조 API")
@RestController
@RequestMapping("/game-test/api")
@RequiredArgsConstructor
public class GameTestAuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final GameTestDevLoginService gameTestDevLoginService;

    /**
     * 테스트 매치 WebSocket 연결용 Access Token을 발급한다.
     *
     * <p>userId는 {@link GameTestMatchService}의 테스트 투수 ID와 동일하다.</p>
     */
    @GetMapping("/ws-token")
    public GameTestWsTokenResponse issueTestWsToken() {
        Long testUserId = GameTestMatchService.TEST_WS_USER_ID;
        String accessToken = jwtTokenProvider.createAccessToken(testUserId, UserRole.USER.name());
        return new GameTestWsTokenResponse(
                accessToken,
                testUserId,
                jwtTokenProvider.getAccessTokenExpiresInSeconds());
    }

    /**
     * 개발자 PIN으로 DevTester 계정에 로그인한다.
     *
     * <p>응답은 OAuth 로그인과 동일한 {@link LoginResponse}이다.
     * 로그인 시 미보유 마스터 구종을 모두 지급한다.</p>
     *
     * @param request PIN
     * @return access/refresh 토큰
     */
    @Operation(summary = "개발자 PIN 로그인", description = "game-test 전용. PIN 8572 → DevTester")
    @PostMapping("/dev-login")
    public LoginResponse devLogin(@Valid @RequestBody GameTestDevLoginRequest request) {
        return gameTestDevLoginService.login(request.pin());
    }
}
