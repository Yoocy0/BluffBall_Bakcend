package com.project.bluffball.global.security;

import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.validator.MatchParticipantValidator;
import com.project.bluffball.global.exception.AuthUnauthorizedException;
import com.project.bluffball.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * 게임 WebSocket 구독·메시지 전송 시 매치 참가자 권한을 검증한다.
 *
 * <p>조회는 {@link MatchInfoReader}, 조건 검증은 {@link MatchParticipantValidator}에 위임한다.</p>
 */
@Component
@RequiredArgsConstructor
public class GameWebSocketSecurityVerifier {

    /** 인게임 브로드캐스트 구독 prefix — {@code /topic/game/{matchSessionId}/...} */
    private static final String GAME_TOPIC_PREFIX = "/topic/game/";

    /** 클라이언트 → 서버 전송 prefix — {@code /app/game/{matchSessionId}/...} */
    private static final String APP_GAME_PREFIX = "/app/game/";

    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final MatchInfoReader matchInfoReader;
    private final MatchParticipantValidator matchParticipantValidator;

    /**
     * STOMP SUBSCRIBE·SEND 시 destination이 게임 경로이면 참가자 여부를 검증한다.
     *
     * @param destination         STOMP destination
     * @param authenticatedUserId JWT로 인증된 userId
     */
    public void verifyGameAccessIfApplicable(String destination, Long authenticatedUserId) {
        String matchSessionId = extractMatchSessionId(destination);
        if (matchSessionId == null) {
            return;
        }
        matchParticipantValidator.validateParticipant(
                matchInfoReader.isParticipant(matchSessionId, authenticatedUserId));
    }

    /**
     * STOMP 헤더 accessor에서 인증된 userId를 추출한다.
     *
     * @param accessor STOMP 헤더 접근자
     * @return CONNECT 시 설정된 userId
     */
    public Long requireUserIdFromSession(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new AuthUnauthorizedException(ErrorCode.AUTH_WS_SESSION_MISSING);
        }
        return authenticatedUserResolver.requireUserId(accessor.getUser());
    }

    /**
     * 게임 topic·app destination에서 matchSessionId를 파싱한다.
     *
     * @param destination STOMP destination
     * @return matchSessionId — 게임 경로가 아니면 {@code null}
     */
    public String extractMatchSessionId(String destination) {
        if (destination == null) {
            return null;
        }
        String remainder;
        if (destination.startsWith(GAME_TOPIC_PREFIX)) {
            remainder = destination.substring(GAME_TOPIC_PREFIX.length());
        } else if (destination.startsWith(APP_GAME_PREFIX)) {
            remainder = destination.substring(APP_GAME_PREFIX.length());
        } else {
            return null;
        }
        if (remainder.isEmpty()) {
            return null;
        }
        int slashIndex = remainder.indexOf('/');
        return slashIndex >= 0 ? remainder.substring(0, slashIndex) : remainder;
    }
}
