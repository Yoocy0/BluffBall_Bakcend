package com.project.bluffball.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 발급·검증 설정.
 *
 * <p>{@code application.yml}의 {@code jwt.*} 값을 바인딩한다.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HMAC 서명에 사용할 비밀 키 (256bit 이상 권장) */
    private String secret;

    /** Access Token 만료 시간 (밀리초) */
    private long accessTokenExpirationMs;
}
