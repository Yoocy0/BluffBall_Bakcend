package com.project.bluffball.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Play Billing·재화 상품 설정.
 *
 * <p>{@code application.yml}의 {@code billing.*} 값을 바인딩한다.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "billing")
public class BillingProperties {

    /** Google Play 검증 설정 */
    private Google google = new Google();

    /** 인앱 재화 상품 목록 (productId → 지급 재화) */
    private List<CurrencyProduct> currencyProducts = new ArrayList<>();

    /**
     * Google Play 패키지·검증 모드·서비스 계정.
     */
    @Getter
    @Setter
    public static class Google {

        /**
         * 검증 모드.
         * <ul>
         *   <li>MOCK — 로컬/개발 (Play API 호출 없음)</li>
         *   <li>LIVE — Android Publisher API로 purchaseToken 검증</li>
         * </ul>
         */
        private String verificationMode = "MOCK";

        /** Play Console 앱 패키지명 */
        private String packageName = "";

        /** 서비스 계정 JSON 전체 문자열 (LIVE 필수) */
        private String serviceAccountJson = "";
    }

    /**
     * 재화 인앱 상품.
     */
    @Getter
    @Setter
    public static class CurrencyProduct {

        /** Play Console productId (SKU) */
        private String productId;

        /** 지급 재화량 */
        private long currencyAmount;
    }
}
