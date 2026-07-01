package com.project.bluffball.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI 3) 설정.
 *
 * <p>문서 접근 경로: {@code /swagger-ui/index.html}</p>
 *
 * <p>WebSocket(STOMP) 엔드포인트는 OpenAPI 스펙 범위 밖이므로
 * {@link com.project.bluffball.domain.game.controller.GameWebSocketController}의
 * Javadoc 주석으로 대체 문서화한다.</p>
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI bluffBallOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BluffBall API")
                        .description("""
                                블러프볼(BluffBall) 백엔드 API 명세서.
                                
                                ## 인증
                                - REST API: `Authorization: Bearer {JWT Access Token}` 헤더 사용
                                - WebSocket: STOMP CONNECT 프레임의 `Authorization` 헤더 사용
                                - `/topic/user/{userId}/...` 구독 시 JWT userId와 경로 userId 일치 필수
                                - `/topic/game/{matchSessionId}/...` 구독·`/app/game/...` 전송 시 해당 매치 참가자만 허용
                                
                                ## WebSocket 엔드포인트 (STOMP)
                                OpenAPI 스펙에 포함되지 않는 WebSocket 엔드포인트는 아래와 같다.
                                
                                | 방향 | 경로 | 설명 |
                                |------|------|------|
                                | 연결 | `/ws` (SockJS) | STOMP 연결 엔드포인트 |
                                | 수신 | `/app/game/{matchSessionId}/setup-numbers` | 블러핑 숫자 제출 |
                                | 수신 | `/app/game/{matchSessionId}/cards/mulligan` | 투수 카드 교체 확정 |
                                | 수신 | `/app/game/{matchSessionId}/pitcher/select-card` | 투수 구종+좌표 선택 |
                                | 수신 | `/app/game/{matchSessionId}/batter/select-card` | 타자 좌표+타이밍 선택 |
                                | 송신 | `/topic/game/{matchSessionId}` | 카드패·준비 이벤트 브로드캐스트 |
                                | 송신 | `/topic/game/{matchSessionId}/result` | 턴 결과·경기 종료 브로드캐스트 |
                                | 송신 | `/topic/user/{userId}/match` | 매칭 성사 알림 (개인 토픽, JWT userId 일치 필수) |
                                """)
                        .version("v0.1.0 (MVP - Single Mode)"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Access Token. 로그인 API 응답에서 발급.")));
    }
}
