package com.project.bluffball.global.security;

import com.project.bluffball.global.exception.AuthForbiddenException;
import com.project.bluffball.global.exception.AuthUnauthorizedException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;

/**
 * SecurityContext·WebSocket Principal에서 인증된 userId를 추출하고,
 * 개인화 WebSocket 토픽 구독 시 경로 userId와 본인 일치 여부를 검증한다.
 */
@Component
public class AuthenticatedUserResolver {

    /** 개인 토픽 destination prefix — {@code /topic/user/{userId}/...} */
    private static final String USER_TOPIC_PREFIX = "/topic/user/";

    /**
     * REST API 요청에서 인증된 userId를 반환한다.
     *
     * <p>JWT 필터가 SecurityContext principal에 Long userId를 설정한다.</p>
     *
     * @return JWT subject userId
     * @throws AuthUnauthorizedException 인증 정보가 없거나 userId 형식이 아닐 때
     */
    public Long requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthUnauthorizedException(ErrorCode.AUTH_REQUIRED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof Principal namedPrincipal) {
            return requireUserId(namedPrincipal);
        }
        throw new AuthUnauthorizedException(ErrorCode.AUTH_INVALID);
    }

    /**
     * WebSocket {@link Principal}에서 인증된 userId를 반환한다.
     *
     * <p>STOMP CONNECT 시 {@code accessor.setUser}로 설정된 principal을 사용한다.</p>
     *
     * @param principal STOMP 세션 principal
     * @return JWT subject userId
     * @throws AuthUnauthorizedException principal이 없거나 userId 형식이 아닐 때
     */
    public Long requireUserId(Principal principal) {
        if (principal == null || !StringUtils.hasText(principal.getName())) {
            throw new AuthUnauthorizedException(ErrorCode.AUTH_WS_REQUIRED);
        }
        return parseUserId(principal.getName());
    }

    /**
     * WebSocket 개인 토픽 구독 시 경로의 userId가 인증 주체와 일치하는지 검증한다.
     *
     * <p>다른 유저의 userId를 경로에 넣어 구독하는 변조를 차단한다.</p>
     *
     * @param destination         STOMP 구독 destination (예: {@code /topic/user/42/match})
     * @param authenticatedUserId CONNECT 시 JWT로 인증된 userId
     * @throws AuthForbiddenException 경로 userId가 본인과 다를 때
     */
    public void verifyUserTopicSubscription(String destination, Long authenticatedUserId) {
        Long pathUserId = extractUserIdFromTopic(destination);
        if (pathUserId == null) {
            return;
        }
        if (!pathUserId.equals(authenticatedUserId)) {
            throw new AuthForbiddenException(ErrorCode.AUTH_CHANNEL_FORBIDDEN);
        }
    }

    /**
     * principal name 문자열을 userId Long으로 파싱한다.
     *
     * @param principalName CONNECT 시 저장된 userId 문자열
     * @return 파싱된 userId
     */
    private Long parseUserId(String principalName) {
        try {
            return Long.parseLong(principalName);
        } catch (NumberFormatException ex) {
            throw new AuthUnauthorizedException(ErrorCode.AUTH_INVALID);
        }
    }

    /**
     * destination이 개인 유저 토픽 형식인지 판별한다.
     *
     * @param destination STOMP 구독 destination
     * @return {@code /topic/user/}로 시작하면 {@code true}
     */
    public boolean isUserTopic(String destination) {
        return destination != null && destination.startsWith(USER_TOPIC_PREFIX);
    }

    /**
     * 개인 토픽 destination에서 userId를 파싱한다.
     *
     * @param destination STOMP 구독 destination
     * @return 파싱된 userId — 개인 토픽이 아니면 {@code null}
     * @throws AuthForbiddenException userId 세그먼트가 숫자가 아닐 때
     */
    private Long extractUserIdFromTopic(String destination) {
        if (!isUserTopic(destination)) {
            return null;
        }
        String remainder = destination.substring(USER_TOPIC_PREFIX.length());
        int slashIndex = remainder.indexOf('/');
        String userIdSegment = slashIndex >= 0 ? remainder.substring(0, slashIndex) : remainder;
        try {
            return Long.parseLong(userIdSegment);
        } catch (NumberFormatException ex) {
            throw new AuthForbiddenException(ErrorCode.AUTH_INVALID_SUBSCRIPTION_PATH);
        }
    }
}
