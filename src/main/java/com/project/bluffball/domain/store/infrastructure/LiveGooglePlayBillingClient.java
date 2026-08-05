package com.project.bluffball.domain.store.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.project.bluffball.global.config.BillingProperties;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Google Play Android Publisher API로 인앱 구매를 검증한다.
 *
 * <p>{@code billing.google.verification-mode=LIVE} 일 때 활성화.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "billing.google", name = "verification-mode", havingValue = "LIVE")
@RequiredArgsConstructor
public class LiveGooglePlayBillingClient implements GooglePlayBillingClient {

    private static final String ANDROID_PUBLISHER_SCOPE =
            "https://www.googleapis.com/auth/androidpublisher";

    /** Billing 설정 */
    private final BillingProperties billingProperties;

    /** HTTP 클라이언트 */
    private final RestClient restClient;

    /** JSON 파서 */
    private final ObjectMapper objectMapper;

    @Override
    public GooglePlayPurchaseVerification verifyProductPurchase(String productId, String purchaseToken) {
        String packageName = requirePackageName();
        String accessToken = accessToken();
        String url = String.format(
                "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/%s/purchases/products/%s/tokens/%s",
                packageName, productId, purchaseToken);
        try {
            String body = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            int purchaseState = root.path("purchaseState").asInt(-1);
            // 0 = Purchased
            if (purchaseState != 0) {
                return GooglePlayPurchaseVerification.invalid("purchaseState=" + purchaseState);
            }
            String orderId = root.path("orderId").asText(null);
            if (orderId != null && orderId.isBlank()) {
                orderId = null;
            }
            return GooglePlayPurchaseVerification.valid(orderId);
        } catch (RestClientResponseException ex) {
            log.warn("Google Play verify failed: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return GooglePlayPurchaseVerification.invalid("Play API " + ex.getStatusCode());
        } catch (Exception ex) {
            throw new ServiceUnavailableException(ErrorCode.STORE_BILLING_UNAVAILABLE, ex.getMessage(), ex);
        }
    }

    @Override
    public void acknowledgeProductPurchase(String productId, String purchaseToken) {
        String packageName = requirePackageName();
        String accessToken = accessToken();
        String url = String.format(
                "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/%s/purchases/products/%s/tokens/%s:acknowledge",
                packageName, productId, purchaseToken);
        try {
            restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}")
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            // 이미 acknowledge된 경우 등 — 지급은 멱등으로 보호되므로 로그만
            log.warn("Google Play acknowledge failed: status={} body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.warn("Google Play acknowledge error: {}", ex.getMessage());
        }
    }

    private String requirePackageName() {
        String packageName = billingProperties.getGoogle().getPackageName();
        if (packageName == null || packageName.isBlank()) {
            throw new ServiceUnavailableException(ErrorCode.STORE_BILLING_UNAVAILABLE, "package-name empty");
        }
        return packageName;
    }

    private String accessToken() {
        String json = billingProperties.getGoogle().getServiceAccountJson();
        if (json == null || json.isBlank()) {
            throw new ServiceUnavailableException(
                    ErrorCode.STORE_BILLING_UNAVAILABLE, "service-account-json empty");
        }
        try {
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
                    .createScoped(List.of(ANDROID_PUBLISHER_SCOPE));
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (Exception ex) {
            throw new ServiceUnavailableException(ErrorCode.STORE_BILLING_UNAVAILABLE, ex.getMessage(), ex);
        }
    }
}
