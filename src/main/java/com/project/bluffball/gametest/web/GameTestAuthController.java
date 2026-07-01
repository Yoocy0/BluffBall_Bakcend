package com.project.bluffball.gametest.web;

import com.project.bluffball.domain.auth.infrastructure.jwt.JwtTokenProvider;
import com.project.bluffball.domain.user.enums.UserRole;
import com.project.bluffball.gametest.dto.GameTestWsTokenResponse;
import com.project.bluffball.gametest.service.GameTestMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게임 테스트 화면용 인증 보조 API.
 *
 * <p>OAuth 없이 WebSocket CONNECT JWT가 필요할 때 테스트 유저 토큰을 발급한다.
 * {@code /game-test/**} 경로 전용이며, 운영 본番 플로우에는 사용하지 않는다.</p>
 */
@RestController
@RequestMapping("/game-test/api")
@RequiredArgsConstructor
public class GameTestAuthController {

    private final JwtTokenProvider jwtTokenProvider;

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
}
