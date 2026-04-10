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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


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
 * <p>Example usage:</p>
 * <pre>
 * PaymentProviderConfig config = PaymentProviderConfig.builder()
 *     .providerName("Sberbank")
 *     .paymentUrl("https://securepayments.sberbank.ru/rest/register.do")
 *     .apiKey("your-api-key")
 *     .feePercent(BigDecimal.valueOf(1.5))
 *     .build();
 *
 * UniversalPaymentGateway gateway = new UniversalPaymentGateway(config, paymentConfig, messageService);
 * PaymentResult result = gateway.processPayment(paymentMethod, amount, "RUB", "Order payment");
 * </pre>
 *
 * @author Household Store Team
 * @see PaymentGateway
 * @see PaymentProviderConfig
 * @see PaymentConfig
 * @since 1.0
 */
@Slf4j
public class UniversalPaymentGateway implements PaymentGateway {

    /** Fallback path for payment URL when not provided in response */
    private static final String FALLBACK_PAYMENT_URL_PATH = "/payment/";

    private final PaymentProviderConfig config;
    private final RestTemplate restTemplate;
    private final PaymentConfig paymentConfig;
    private final MessageService messageService;

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

    /**
     * Processes a payment through the configured payment provider.
     *
     * <p>This method performs the following steps:</p>
     * <ol>
     *   <li>Generates a unique transaction ID</li>
     *   <li>Builds an HTTP request with payment details and authentication</li>
     *   <li>Sends the request to the provider's payment endpoint</li>
     *   <li>Parses the response and extracts transaction ID and payment URL</li>
     *   <li>Calculates fees and net amount</li>
     * </ol>
     *
     * @param paymentMethod the payment method to use (card, wallet, etc.)
     * @param amount        the payment amount
     * @param currency      the currency code (e.g., "RUB", "USD")
     * @param description   the payment description
     * @return a {@link PaymentResult} containing the result of the payment operation
     */
    @Override
    public PaymentResult processPayment(PaymentMethod paymentMethod, BigDecimal amount,
                                        String currency, String description) {
        try {
            String transactionId = generateTransactionId();

            log.info(messageService.get("payment.gateway.processing.start",
                    config.getProviderName(), transactionId, amount, currency));

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
                        .errorMessage("Payment failed: " + response.getBody())
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

    /**
     * Processes a refund for a previously completed payment.
     *
     * <p>This method sends a refund request to the payment provider's refund endpoint.
     * The original transaction ID is required to identify the payment to be refunded.</p>
     *
     * @param paymentMethod the payment method used for the original transaction
     * @param transactionId the provider's transaction ID of the original payment
     * @param amount        the amount to refund
     * @param reason        the reason for the refund
     * @return a {@link PaymentResult} containing the result of the refund operation
     */
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
                        .errorMessage("Refund failed: " + response.getBody())
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

    /**
     * Checks the status of a payment transaction.
     *
     * <p>This method queries the payment provider's status endpoint to get the current
     * state of a payment (PENDING, COMPLETED, FAILED, etc.).</p>
     *
     * @param transactionId the provider's transaction ID to check
     * @return the current {@link PaymentStatus} of the transaction
     */
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

    /**
     * Builds an HTTP request entity for a payment operation.
     *
     * <p>This method constructs the request with appropriate headers (including
     * authentication) and a JSON body containing payment details.</p>
     *
     * @param paymentMethod the payment method
     * @param amount        the payment amount
     * @param currency      the currency code
     * @param description   the payment description
     * @return an {@link HttpEntity} containing the request headers and body
     */
    private HttpEntity<?> buildPaymentRequest(PaymentMethod paymentMethod, BigDecimal amount,
                                              String currency, String description) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (config.getApiKey() != null) {
            headers.set("Authorization", config.getAuthScheme() + " " + config.getApiKey());
            log.debug(messageService.get("payment.gateway.request.auth.added", config.getProviderName()));
        }
        if (config.getApiSecret() != null) {
            headers.set("X-API-Secret", config.getApiSecret());
        }

        Map<String, Object> body = new HashMap<>();
        body.putAll(config.getPaymentParams());
        body.put("amount", amount);
        body.put("currency", currency);
        body.put("description", description);
        body.put("payment_method_id", paymentMethod.getId());

        if (paymentMethod.getMaskedIdentifier() != null) {
            body.put("payment_identifier", paymentMethod.getMaskedIdentifier());
        }

        log.debug(messageService.get("payment.gateway.request.built", config.getProviderName()));
        return new HttpEntity<>(body, headers);
    }

    /**
     * Builds an HTTP request entity for a refund operation.
     *
     * @param transactionId the original transaction ID
     * @param amount        the refund amount
     * @param reason        the refund reason
     * @return an {@link HttpEntity} containing the request headers and body
     */
    private HttpEntity<?> buildRefundRequest(String transactionId, BigDecimal amount, String reason) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (config.getApiKey() != null) {
            headers.set("Authorization", config.getAuthScheme() + " " + config.getApiKey());
        }

        Map<String, Object> body = new HashMap<>();
        body.putAll(config.getRefundParams());
        body.put("transaction_id", transactionId);
        body.put("amount", amount);
        body.put("reason", reason);

        log.debug(messageService.get("payment.gateway.refund.request.built", config.getProviderName()));
        return new HttpEntity<>(body, headers);
    }

    /**
     * Builds an HTTP request entity for a status check operation.
     *
     * @param transactionId the transaction ID to check
     * @return an {@link HttpEntity} containing the request headers and body
     */
    private HttpEntity<?> buildStatusRequest(String transactionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (config.getApiKey() != null) {
            headers.set("Authorization", config.getAuthScheme() + " " + config.getApiKey());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("transaction_id", transactionId);

        return new HttpEntity<>(body, headers);
    }

    /**
     * Generates a unique transaction ID for the payment.
     *
     * <p>The format is: {prefix}-{timestamp}-{random-string}</p>
     * The random string length is configurable via {@link PaymentConfig}.
     *
     * @return a unique transaction ID string
     */
    private String generateTransactionId() {
        Integer randomLength = paymentConfig.getProcessing().getTransactionIdRandomLength();
        int length = randomLength != null ? randomLength : 8;

        String transactionId = config.getTransactionPrefix() + "-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, length).toUpperCase();

        log.debug(messageService.get("payment.gateway.transaction.id.generated",
                config.getProviderName(), transactionId));

        return transactionId;
    }

    /**
     * Extracts the provider transaction ID from the response body.
     *
     * <p><b>Note:</b> This is a placeholder implementation. In production,
     * this should parse the JSON response according to the provider's API specification.</p>
     *
     * @param responseBody the response body from the provider
     * @return the extracted transaction ID
     */
    private String extractTransactionId(String responseBody) {
        // TODO: Implement JSON parsing based on provider
        log.debug(messageService.get("payment.gateway.extract.transaction.id.placeholder"));
        return FALLBACK_TXN_PREFIX + System.currentTimeMillis();
    }

    /**
     * Extracts the payment URL from the response body for redirect-based payments.
     *
     * <p><b>Note:</b> This is a placeholder implementation. In production,
     * this should parse the JSON response according to the provider's API specification.</p>
     *
     * @param responseBody the response body from the provider
     * @return the payment URL for customer redirect
     */
    private String extractPaymentUrl(String responseBody) {
        // TODO: Extract payment URL from response
        String returnUrl = config.getReturnUrl();
        String baseUrl = returnUrl != null ? returnUrl : "";
        String paymentUrl = baseUrl + FALLBACK_PAYMENT_URL_PATH + System.currentTimeMillis();

        log.debug(messageService.get("payment.gateway.extract.payment.url.placeholder", paymentUrl));
        return paymentUrl;
    }

    /**
     * Extracts the payment status from the response body.
     *
     * <p><b>Note:</b> This is a placeholder implementation. In production,
     * this should parse the JSON response according to the provider's API specification.</p>
     *
     * @param responseBody the response body from the provider
     * @return the extracted {@link PaymentStatus}
     */
    private PaymentStatus extractStatus(String responseBody) {
        // TODO: Parse status from response
        log.debug(messageService.get("payment.gateway.extract.status.placeholder"));
        return PaymentStatus.COMPLETED;
    }

    /**
     * Calculates the fee for a transaction based on the provider's fee structure.
     *
     * <p>If a percentage fee is configured, it is calculated as a percentage of the amount.
     * If a fixed fee is configured, it is used directly. Percentage fees take precedence.</p>
     *
     * @param amount the transaction amount
     * @return the calculated fee amount
     */
    private BigDecimal calculateFee(BigDecimal amount) {
        if (config.getFeePercent() != null) {
            BigDecimal fee = amount.multiply(config.getFeePercent())
                    .divide(BigDecimal.valueOf(paymentConfig.getProcessing().getPercentBase()));
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

    /**
     * Calculates the net amount after fees.
     *
     * @param amount the transaction amount
     * @return the net amount (amount - fee)
     */
    private BigDecimal calculateNetAmount(BigDecimal amount) {
        BigDecimal fee = calculateFee(amount);
        BigDecimal netAmount = amount.subtract(fee);

        log.debug(messageService.get("payment.gateway.net.amount.calculated",
                config.getProviderName(), amount, fee, netAmount));

        return netAmount;
    }
}