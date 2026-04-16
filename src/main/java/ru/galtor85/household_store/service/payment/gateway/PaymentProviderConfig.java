package ru.galtor85.household_store.service.payment.gateway;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpMethod;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static ru.galtor85.household_store.constants.TechnicalConstants.TOKEN_TYPE;

/**
 * Configuration for a payment provider
 *
 * <p>This class holds all necessary configuration for integrating with
 * various payment service providers like Sberbank, YooMoney, Qiwi, PayPal, Stripe, etc.</p>
 */
@Data
@Builder
public class PaymentProviderConfig {

    // =========================================================================
    // PROVIDER IDENTIFICATION
    // =========================================================================

    /**
     * Display name of the payment provider (e.g., "Sberbank", "YooMoney")
     */
    private String providerName;

    /**
     * Internal code for the provider (e.g., "sberbank", "yoomoney", "qiwi")
     */
    private String providerCode;

    // =========================================================================
    // API ENDPOINTS
    // =========================================================================

    /**
     * URL for creating a payment
     */
    private String paymentUrl;

    /**
     * URL for processing refunds
     */
    private String refundUrl;

    /**
     * URL for checking payment status
     */
    private String statusUrl;

    // =========================================================================
    // AUTHENTICATION
    // =========================================================================

    /**
     * API key for authentication with the provider
     */
    private String apiKey;

    /**
     * API secret for authentication with the provider
     */
    private String apiSecret;

    /**
     * Authentication scheme (e.g., "Bearer", "Basic", "Api-Key")
     */
    @Builder.Default
    private String authScheme = TOKEN_TYPE;

    // =========================================================================
    // REQUEST CONFIGURATION
    // =========================================================================

    /**
     * HTTP method for API requests (POST, GET, PUT, etc.)
     */
    @Builder.Default
    private HttpMethod httpMethod = HttpMethod.POST;

    /**
     * Additional HTTP headers for API requests
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    /**
     * Additional parameters for payment requests
     */
    @Builder.Default
    private Map<String, Object> paymentParams = new HashMap<>();

    /**
     * Additional parameters for refund requests
     */
    @Builder.Default
    private Map<String, Object> refundParams = new HashMap<>();

    // =========================================================================
    // FEE CONFIGURATION
    // =========================================================================

    /**
     * Percentage fee charged by the provider (e.g., 2.9 for 2.9%)
     */
    private BigDecimal feePercent;

    /**
     * Fixed fee charged by the provider (e.g., 0.30 for $0.30)
     */
    private BigDecimal feeFixed;

    // =========================================================================
    // TRANSACTION CONFIGURATION
    // =========================================================================

    /**
     * Prefix for generated transaction IDs (e.g., "SB", "YM", "PP")
     */
    private String transactionPrefix;

    /**
     * Return URL where users are redirected after successful payment
     */
    private String returnUrl;

    /**
     * Webhook URL for payment notifications from the provider
     */
    private String webhookUrl;

    // =========================================================================
    // FEATURE FLAGS
    // =========================================================================

    /**
     * Whether the provider supports recurring/subscription payments
     */
    @Builder.Default
    private boolean supportsRecurring = false;

    /**
     * Whether the provider supports refunds
     */
    @Builder.Default
    private boolean supportsRefunds = true;

    /**
     * Whether the provider supports webhook notifications
     */
    @Builder.Default
    private boolean supportsWebhooks = false;

    // =========================================================================
    // TIMEOUTS
    // =========================================================================

    /**
     * Connection timeout in milliseconds
     */
    @Builder.Default
    private int connectTimeout = 30000;

    /**
     * Read timeout in milliseconds
     */
    @Builder.Default
    private int readTimeout = 30000;
}