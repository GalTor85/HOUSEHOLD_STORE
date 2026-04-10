package ru.galtor85.household_store.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

/**
 * Configuration for payment processing settings.
 *
 * <p>This class holds all payment-related configuration including processing settings
 * (fee calculation, rounding) and provider-specific settings for various payment
 * gateways (Sberbank, YooMoney, Qiwi, PayPal, Stripe).</p>
 *
 * <p>All values are loaded from application.properties with prefix 'app.payment'.</p>
 *
 * <p><b>Example configuration in application.properties:</b></p>
 * <pre>
 * app.payment.processing.fee-scale=2
 * app.payment.processing.rounding-mode=HALF_UP
 * app.payment.processing.percent-base=100
 * app.payment.processing.transaction-id-random-length=8
 *
 * app.payment.providers.sberbank.fee-percent=1.5
 * app.payment.providers.sberbank.payment-url=<a href="https://securepayments.sberbank.ru/rest/register.do">...</a>
 * app.payment.providers.sberbank.transaction-prefix=SB
 * </pre>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.payment")
public class PaymentConfig {

    /** Payment processing configuration */
    private ProcessingConfig processing = new ProcessingConfig();

    /** Payment providers configuration */
    private ProvidersConfig providers = new ProvidersConfig();

    /**
     * Configuration for payment processing (fee calculation, rounding, etc.)
     */
    @Data
    public static class ProcessingConfig {
        /** Scale (number of decimal places) for fee calculation */
        private Integer feeScale;

        /** Rounding mode for fee calculation (HALF_UP, HALF_DOWN, etc.) */
        private RoundingMode roundingMode;

        /** Base percentage value (100 = 100%) for fee calculation */
        private Integer percentBase;

        /** Length of random suffix for transaction ID generation */
        private Integer transactionIdRandomLength = 8;
    }

    /**
     * Configuration for payment providers.
     * Each provider can have its own settings for fees, URLs, timeouts, etc.
     */
    @Data
    public static class ProvidersConfig {
        /** Sberbank payment provider configuration */
        private ProviderConfig sberbank = new ProviderConfig();

        /** YooMoney payment provider configuration */
        private ProviderConfig yoomoney = new ProviderConfig();

        /** Qiwi payment provider configuration */
        private ProviderConfig qiwi = new ProviderConfig();

        /** PayPal payment provider configuration */
        private ProviderConfig paypal = new ProviderConfig();

        /** Stripe payment provider configuration */
        private ProviderConfig stripe = new ProviderConfig();

        /**
         * Configuration for a specific payment provider.
         */
        @Data
        public static class ProviderConfig {
            // =========================================================================
            // FEE SETTINGS
            // =========================================================================

            /** Percentage fee charged by the provider (e.g., 1.5 for 1.5%) */
            private Double feePercent;

            /** Fixed fee charged by the provider (e.g., 0.30 for $0.30) */
            private Double feeFixed;

            // =========================================================================
            // TIMEOUT SETTINGS
            // =========================================================================

            /** Connection timeout in milliseconds */
            private Integer connectTimeout;

            /** Read timeout in milliseconds */
            private Integer readTimeout;

            // =========================================================================
            // URL SETTINGS
            // =========================================================================

            /** Payment endpoint URL */
            private String paymentUrl;

            /** Refund endpoint URL */
            private String refundUrl;

            /** Status check endpoint URL */
            private String statusUrl;

            /** Prefix for generated transaction IDs */
            private String transactionPrefix;

            /** Return URL for customer after payment */
            private String returnUrl;

            /** Webhook URL for payment notifications */
            private String webhookUrl;
        }
    }
}