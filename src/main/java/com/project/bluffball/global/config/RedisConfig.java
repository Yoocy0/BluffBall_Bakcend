package com.project.bluffball.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Redis 설정.
 * 연결 정보는 application.yml의 spring.data.redis.*로 자동 구성된다.
 * @EnableRedisRepositories: @RedisHash 기반 Repository 활성화
 */
@Configuration
@EnableRedisRepositories(basePackages = "com.project.bluffball")
public class RedisConfig {
}
