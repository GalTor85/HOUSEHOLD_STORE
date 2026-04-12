package ru.galtor85.household_store.service.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import ru.galtor85.household_store.config.PaymentConfig;
import ru.galtor85.household_store.entity.payment.PaymentMethod;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.payment.PaymentGateway;
import ru.galtor85.household_store.service.payment.PaymentResult;
import ru.galtor85.household_store.service.payment.PaymentStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static ru.galtor85.household_store.constants.PaymentConstants.*;
import static ru.galtor85.household_store.constants.TechnicalConstants.REFUND_TXN_PREFIX;
import static ru.galtor85.household_store.constants.TechnicalConstants.FALLBACK_TXN_PREFIX;

/**
 * Universal payment gateway that can be configured for different providers.
 *
 * <p>This class provides a flexible implementation of the {@link PaymentGateway} interface
 * that can work with various payment service providers (Sberbank, YooMoney, Qiwi, PayPal, Stripe, etc.)
 * through external configuration.</p>
 *
 * <p>The gateway uses a {@link PaymentProviderConfig} object that contains all provider-specific
 * settings including API endpoints, authentication credentials, fee structures, and timeout
 * configurations. This design allows adding new payment providers without modifying the
 * gateway implementation.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Dynamic configuration for different payment providers</li>
 *   <li>Automatic fee calculation based on provider's fee structure</li>
 *   <li>Support for payment processing, refunds, and status checking</li>
 *   <li>Transaction ID generation with configurable random length</li>
 *   <li>HTTP request building with proper authentication headers</li>
 * </ul>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
public class UniversalPaymentGateway implements PaymentGateway {

    private static final String PAYMENT_FAILED_PREFIX = "Payment failed: ";
    private static final String REFUND_FAILED_PREFIX = "Refund failed: ";

    // =========================================================================
    // FIELDS
    // =========================================================================

    private final PaymentProviderConfig config;
    private final RestTemplate restTemplate;
    private final PaymentConfig paymentConfig;
    private final MessageService messageService;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    /**
     * Constructs a new UniversalPaymentGateway with the specified configuration.
     *
     * @param config         the provider-specific configuration
     * @param paymentConfig  the global payment configuration
     * @param messageService the message service for localization
     */
    public UniversalPaymentGateway(PaymentProviderConfig config,
                                   PaymentConfig paymentConfig,
                                   MessageService messageService) {
        this.config = config;
        this.paymentConfig = paymentConfig;
        this.messageService = messageService;
        this.restTemplate = new RestTemplate();
    }

    // =========================================================================
    // PAYMENT PROCESSING
    // =========================================================================

    @Override
    public PaymentResult processPayment(PaymentMethod paymentMethod, BigDecimal amount,
                                        String currency, String description) {
        try {
            String transactionId = generateTransactionId();

            log.info(messageService.get("payment.gateway.processing.start",
                    config.getProviderName(), transactionId, amount, currency));

            // Handle offline payment providers (e.g., cash)
            if (isOfflineProvider()) {
                log.info(messageService.get("payment.gateway.offline.processing", config.getProviderName()));
                return buildOfflinePaymentResult(transactionId, amount);
            }

            // Validate payment URL for online providers
            if (config.getPaymentUrl() == null || config.getPaymentUrl().isEmpty()) {
                throw new IllegalStateException(
                        messageService.get("payment.gateway.url.not.configured", config.getProviderName())
                );
            }

            HttpEntity<?> requestEntity = buildPaymentRequest(paymentMethod, amount, currency, description);
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getPaymentUrl(),
                    config.getHttpMethod(),
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                String providerTransactionId = extractTransactionId(response.getBody());
                log.info(messageService.get("payment.gateway.processing.success",
                        config.getProviderName(), transactionId));

                return PaymentResult.builder()
                        .success(true)
                        .transactionId(providerTransactionId)
                        .paymentUrl(extractPaymentUrl(response.getBody()))
                        .fee(calculateFee(amount))
                        .netAmount(calculateNetAmount(amount))
                        .build();
            } else {
                log.warn(messageService.get("payment.gateway.processing.http.error",
                        config.getProviderName(), response.getStatusCode(), response.getBody()));

                return PaymentResult.builder()
                        .success(false)
                        .errorMessage(PAYMENT_FAILED_PREFIX + response.getBody())
                        .build();
            }

        } catch (Exception e) {
            log.error(messageService.get("payment.gateway.processing.error",
                    config.getProviderName(), e.getMessage()), e);
            return PaymentResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    // =========================================================================
    // REFUND PROCESSING
    // =========================================================================

    @Override
    public PaymentResult refundPayment(PaymentMethod paymentMethod, String transactionId,
                                       BigDecimal amount, String reason) {
        try {
            String refundId = REFUND_TXN_PREFIX + generateTransactionId();

            log.info(messageService.get("payment.gateway.refund.start",
                    config.getProviderName(), transactionId, amount, reason));

            HttpEntity<?> requestEntity = buildRefundRequest(transactionId, amount, reason);
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getRefundUrl(),
                    config.getHttpMethod(),
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info(messageService.get("payment.gateway.refund.success",
                        config.getProviderName(), refundId));

                return PaymentResult.builder()
                        .success(true)
                        .transactionId(refundId)
                        .build();
            } else {
                log.warn(messageService.get("payment.gateway.refund.http.error",
                        config.getProviderName(), response.getStatusCode(), response.getBody()));

                return PaymentResult.builder()
                        .success(false)
                        .errorMessage(REFUND_FAILED_PREFIX + response.getBody())
                        .build();
            }

        } catch (Exception e) {
            log.error(messageService.get("payment.gateway.refund.error",
                    config.getProviderName(), e.getMessage()), e);
            return PaymentResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    // =========================================================================
    // STATUS CHECKING
    // =========================================================================

    @Override
    public PaymentStatus checkStatus(String transactionId) {
        try {
            log.debug(messageService.get("payment.gateway.status.check.start",
                    config.getProviderName(), transactionId));

            HttpEntity<?> requestEntity = buildStatusRequest(transactionId);
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getStatusUrl(),
                    config.getHttpMethod(),
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                PaymentStatus status = extractStatus(response.getBody());
                log.debug(messageService.get("payment.gateway.status.check.success",
                        config.getProviderName(), transactionId, status));
                return status;
            }

            log.warn(messageService.get("payment.gateway.status.check.http.error",
                    config.getProviderName(), response.getStatusCode()));

            return PaymentStatus.FAILED;

        } catch (Exception e) {
            log.error(messageService.get("payment.gateway.status.check.error",
                    config.getProviderName(), e.getMessage()), e);
            return PaymentStatus.FAILED;
        }
    }

    // =========================================================================
    // REQUEST BUILDERS
    // =========================================================================

    private HttpEntity<?> buildPaymentRequest(PaymentMethod paymentMethod, BigDecimal amount,
                                              String currency, String description) {
        HttpHeaders headers = buildHeaders();
        Map<String, Object> body = buildPaymentBody(paymentMethod, amount, currency, description);

        log.debug(messageService.get("payment.gateway.request.built", config.getProviderName()));
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<?> buildRefundRequest(String transactionId, BigDecimal amount, String reason) {
        HttpHeaders headers = buildHeaders();
        Map<String, Object> body = buildRefundBody(transactionId, amount, reason);

        log.debug(messageService.get("payment.gateway.refund.request.built", config.getProviderName()));
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<?> buildStatusRequest(String transactionId) {
        HttpHeaders headers = buildHeaders();
        Map<String, Object> body = new HashMap<>();
        body.put(JSON_TRANSACTION_ID, transactionId);

        return new HttpEntity<>(body, headers);
    }

    // =========================================================================
    // HEADER BUILDERS
    // =========================================================================

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (config.getApiKey() != null) {
            headers.set(HEADER_AUTHORIZATION, config.getAuthScheme() + " " + config.getApiKey());
            log.debug(messageService.get("payment.gateway.request.auth.added", config.getProviderName()));
        }
        if (config.getApiSecret() != null) {
            headers.set(HEADER_API_SECRET, config.getApiSecret());
        }

        return headers;
    }

    // =========================================================================
    // BODY BUILDERS
    // =========================================================================

    private Map<String, Object> buildPaymentBody(PaymentMethod paymentMethod, BigDecimal amount,
                                                 String currency, String description) {
        Map<String, Object> body = new HashMap<>(config.getPaymentParams());
        body.put(JSON_AMOUNT, amount);
        body.put(JSON_CURRENCY, currency);
        body.put(JSON_DESCRIPTION, description);
        body.put(JSON_PAYMENT_METHOD_ID, paymentMethod.getId());

        if (paymentMethod.getMaskedIdentifier() != null) {
            body.put(JSON_PAYMENT_IDENTIFIER, paymentMethod.getMaskedIdentifier());
        }

        return body;
    }

    private Map<String, Object> buildRefundBody(String transactionId, BigDecimal amount, String reason) {
        Map<String, Object> body = new HashMap<>(config.getRefundParams());
        body.put(JSON_TRANSACTION_ID, transactionId);
        body.put(JSON_AMOUNT, amount);
        body.put(JSON_REASON, reason);

        return body;
    }

    // =========================================================================
    // PROVIDER HELPERS
    // =========================================================================

    private boolean isOfflineProvider() {
        return CASH_PROVIDER_CODE.equals(config.getProviderCode()) ||
                CASH_REGISTER_NAME.equals(config.getProviderName());
    }

    private PaymentResult buildOfflinePaymentResult(String transactionId, BigDecimal amount) {
        return PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .fee(calculateFee(amount))
                .netAmount(calculateNetAmount(amount))
                .paymentUrl(null)
                .build();
    }

    // =========================================================================
    // TRANSACTION ID GENERATION
    // =========================================================================

    private String generateTransactionId() {
        Integer randomLength = paymentConfig.getProcessing().getTransactionIdRandomLength();
        int length = randomLength != null ? randomLength : DEFAULT_RANDOM_LENGTH;

        String transactionId = config.getTransactionPrefix() + "-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, length).toUpperCase();

        log.debug(messageService.get("payment.gateway.transaction.id.generated",
                config.getProviderName(), transactionId));

        return transactionId;
    }

    // =========================================================================
    // RESPONSE EXTRACTORS (placeholder implementations)
    // =========================================================================

    private String extractTransactionId(String responseBody) {
        log.debug(messageService.get("payment.gateway.extract.transaction.id.placeholder"));
        return FALLBACK_TXN_PREFIX + System.currentTimeMillis();
    }

    private String extractPaymentUrl(String responseBody) {
        String returnUrl = config.getReturnUrl();
        String baseUrl = returnUrl != null ? returnUrl : "";
        String paymentUrl = baseUrl + FALLBACK_PAYMENT_URL_PATH + System.currentTimeMillis();

        log.debug(messageService.get("payment.gateway.extract.payment.url.placeholder", paymentUrl));
        return paymentUrl;
    }

    private PaymentStatus extractStatus(String responseBody) {
        log.debug(messageService.get("payment.gateway.extract.status.placeholder"));
        return PaymentStatus.COMPLETED;
    }

    // =========================================================================
    // FEE CALCULATION
    // =========================================================================

    private BigDecimal calculateFee(BigDecimal amount) {
        if (config.getFeePercent() != null) {
            BigDecimal fee = amount.multiply(config.getFeePercent())
                    .divide(BigDecimal.valueOf(paymentConfig.getProcessing().getPercentBase()), RoundingMode.HALF_UP);
            log.debug(messageService.get("payment.gateway.fee.percentage.calculated",
                    config.getProviderName(), fee, config.getFeePercent()));
            return fee;
        }
        if (config.getFeeFixed() != null) {
            log.debug(messageService.get("payment.gateway.fee.fixed.calculated",
                    config.getProviderName(), config.getFeeFixed()));
            return config.getFeeFixed();
        }
        log.debug(messageService.get("payment.gateway.fee.none", config.getProviderName()));
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateNetAmount(BigDecimal amount) {
        BigDecimal fee = calculateFee(amount);
        BigDecimal netAmount = amount.subtract(fee);

        log.debug(messageService.get("payment.gateway.net.amount.calculated",
                config.getProviderName(), amount, fee, netAmount));

        return netAmount;
    }
}