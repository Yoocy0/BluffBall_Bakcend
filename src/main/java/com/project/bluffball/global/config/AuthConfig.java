package com.project.bluffball.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 인증 관련 공통 Bean 설정.
 */
@Configuration
@EnableConfigurationProperties({OAuthProperties.class, JwtProperties.class})
public class AuthConfig {

    /** OAuth API 호출용 HTTP 클라이언트 */
    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}
