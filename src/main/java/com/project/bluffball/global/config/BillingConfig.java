package com.project.bluffball.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Billing 설정 바인딩.
 */
@Configuration
@EnableConfigurationProperties(BillingProperties.class)
public class BillingConfig {
}
