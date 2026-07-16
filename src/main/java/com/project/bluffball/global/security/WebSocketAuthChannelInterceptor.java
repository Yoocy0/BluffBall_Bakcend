package com.project.bluffball.global.security;

import com.project.bluffball.domain.auth.infrastructure.jwt.JwtTokenProvider;
import com.project.bluffball.global.exception.AuthUnauthorizedException;
import com.project.bluffball.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * STOMP CONNECT·SUBSCRIBE·SEND 시 JWT 인증 및 토픽·매치 접근 권한을 검증한다.
 *
 * <p>클라이언트는 CONNECT 프레임 native header에
 * {@code Authorization: Bearer {accessToken}}을 포함해야 한다.</p>
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    /** Authorization 헤더에서 Bearer 접두사 제거용 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** CONNECT 시 JWT 파싱·검증 */
    private final JwtTokenProvider jwtTokenProvider;

    /** 개인 토픽 userId 일치 검증 */
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /** 게임 topic/app 경로 매치 참가자 검증 */
    private final GameWebSocketSecurityVerifier gameWebSocketSecurityVerifier;

    /**
     * STOMP 프레임 전송 전 인증·구독·메시지 권한을 검증한다.
     *
     * @param message STOMP 메시지
     * @param channel 메시지 채널
     * @return 원본 또는 수정된 메시지
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        // CONNECT — JWT 검증 후 세션 principal에 userId 설정
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Long userId = resolveUserIdFromConnect(accessor);
            accessor.setUser(() -> String.valueOf(userId));
        }

        // SUBSCRIBE — 개인 토픽·게임 토픽 접근 권한 검증
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Long authenticatedUserId = gameWebSocketSecurityVerifier.requireUserIdFromSession(accessor);
            authenticatedUserResolver.verifyUserTopicSubscription(
                    accessor.getDestination(),
                    authenticatedUserId);
            gameWebSocketSecurityVerifier.verifyGameAccessIfApplicable(
                    accessor.getDestination(),
                    authenticatedUserId);
        }

        // SEND — /app/game/{matchSessionId}/... 메시지는 참가자만 허용
        if (StompCommand.SEND.equals(accessor.getCommand())) {
            Long authenticatedUserId = gameWebSocketSecurityVerifier.requireUserIdFromSession(accessor);
            gameWebSocketSecurityVerifier.verifyGameAccessIfApplicable(
                    accessor.getDestination(),
                    authenticatedUserId);
        }

        return message;
    }

    /**
     * CONNECT 프레임 native header에서 JWT를 추출·검증하고 userId를 반환한다.
     *
     * @param accessor STOMP 헤더 접근자
     * @return JWT subject userId
     */
    private Long resolveUserIdFromConnect(StompHeaderAccessor accessor) {
        String token = resolveBearerToken(accessor.getFirstNativeHeader("Authorization"));
        if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
            throw new AuthUnauthorizedException(ErrorCode.AUTH_WS_JWT_REQUIRED);
        }
        return jwtTokenProvider.extractUserId(token);
    }

    /**
     * Authorization native header에서 Bearer 토큰 문자열만 추출한다.
     *
     * @param authorizationHeader {@code Authorization} 헤더 값
     * @return JWT 문자열 — 없거나 Bearer 형식이 아니면 {@code null}
     */
    private String resolveBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}
