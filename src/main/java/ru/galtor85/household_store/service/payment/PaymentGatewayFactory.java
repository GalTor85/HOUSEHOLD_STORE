package ru.galtor85.household_store.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.config.PaymentConfig;
import ru.galtor85.household_store.entity.payment.PaymentProvider;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.payment.gateway.PaymentProviderConfig;
import ru.galtor85.household_store.service.payment.gateway.UniversalPaymentGateway;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static ru.galtor85.household_store.constants.TechnicalConstants.TOKEN_TYPE;

/**
 * Factory for creating universal payment gateways.
 *
 * <p>This factory creates and caches {@link UniversalPaymentGateway} instances for
 * different payment providers. All configuration is loaded from {@link PaymentConfig}
 * which reads settings from application.properties.</p>
 *
 * <p>The factory supports dynamic registration and reloading of gateway configurations
 * at runtime, allowing for flexible payment provider management.</p>
 *
 * @author G@LTor85
 * @see UniversalPaymentGateway
 * @see PaymentProviderConfig
 * @see PaymentConfig
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentGatewayFactory {

    private final PaymentConfig paymentConfig;
    private final MessageService messageService;
    private final Map<PaymentProvider, UniversalPaymentGateway> gateways = new ConcurrentHashMap<>();

    /**
     * Retrieves or creates a gateway for the specified payment provider.
     *
     * @param provider the payment provider
     * @return the UniversalPaymentGateway instance for the provider
     */
    public UniversalPaymentGateway getGateway(PaymentProvider provider) {
        return gateways.computeIfAbsent(provider, this::createGateway);
    }

    /**
     * Creates a new gateway for the specified provider using configuration.
     *
     * @param provider the payment provider
     * @return a new UniversalPaymentGateway instance
     */
    private UniversalPaymentGateway createGateway(PaymentProvider provider) {
        PaymentProviderConfig config = getConfigForProvider(provider);

        log.info(messageService.get("payment.gateway.factory.creating",
                provider, config.getPaymentUrl()));

        return new UniversalPaymentGateway(config, paymentConfig, messageService);
    }

    /**
     * Builds provider configuration from application properties.
     *
     * @param provider the payment provider
     * @return PaymentProviderConfig with all settings from properties
     */
    private PaymentProviderConfig getConfigForProvider(PaymentProvider provider) {
        log.debug(messageService.get("payment.gateway.factory.building.config", provider));

        return switch (provider) {
            case SBERBANK -> buildSberbankConfig();
            case YOOMONEY -> buildYooMoneyConfig();
            case QIWI -> buildQiwiConfig();
            case PAYPAL -> buildPayPalConfig();
            case STRIPE -> buildStripeConfig();
            default -> {
                log.error(messageService.get("payment.gateway.factory.unknown.provider", provider));
                throw new IllegalArgumentException(
                        messageService.get("payment.gateway.factory.unknown.provider.error", provider)
                );
            }
        };
    }

    /**
     * Builds configuration for Sberbank payment provider.
     *
     * @return PaymentProviderConfig for Sberbank
     */
    private PaymentProviderConfig buildSberbankConfig() {
        PaymentConfig.ProvidersConfig.ProviderConfig cfg = paymentConfig.getProviders().getSberbank();

        log.debug(messageService.get("payment.gateway.factory.config.sberbank",
                cfg.getPaymentUrl(), cfg.getTransactionPrefix()));

        return PaymentProviderConfig.builder()
                .providerName("Sberbank")
                .providerCode("sberbank")
                .paymentUrl(cfg.getPaymentUrl())
                .refundUrl(cfg.getRefundUrl())
                .statusUrl(cfg.getStatusUrl())
                .apiKey(System.getenv("SBERBANK_API_KEY"))
                .apiSecret(System.getenv("SBERBANK_API_SECRET"))
                .authScheme(TOKEN_TYPE)
                .httpMethod(HttpMethod.POST)
                .feePercent(cfg.getFeePercent() != null ? BigDecimal.valueOf(cfg.getFeePercent()) : null)
                .transactionPrefix(cfg.getTransactionPrefix())
                .returnUrl(cfg.getReturnUrl())
                .webhookUrl(cfg.getWebhookUrl())
                .supportsRefunds(true)
                .supportsWebhooks(true)
                .connectTimeout(cfg.getConnectTimeout() != null ? cfg.getConnectTimeout() : 30000)
                .readTimeout(cfg.getReadTimeout() != null ? cfg.getReadTimeout() : 30000)
                .paymentParams(Map.of(
                        "language", "ru",
                        "pageView", "DESKTOP"
                ))
                .build();
    }

    /**
     * Builds configuration for YooMoney payment provider.
     *
     * @return PaymentProviderConfig for YooMoney
     */
    private PaymentProviderConfig buildYooMoneyConfig() {
        PaymentConfig.ProvidersConfig.ProviderConfig cfg = paymentConfig.getProviders().getYoomoney();

        log.debug(messageService.get("payment.gateway.factory.config.yoomoney",
                cfg.getPaymentUrl(), cfg.getTransactionPrefix()));

        return PaymentProviderConfig.builder()
                .providerName("YooMoney")
                .providerCode("yoomoney")
                .paymentUrl(cfg.getPaymentUrl())
                .refundUrl(cfg.getRefundUrl())
                .statusUrl(cfg.getStatusUrl())
                .apiKey(System.getenv("YOOMONEY_SHOP_ID"))
                .apiSecret(System.getenv("YOOMONEY_SECRET_KEY"))
                .authScheme("Basic")
                .httpMethod(HttpMethod.POST)
                .feePercent(cfg.getFeePercent() != null ? BigDecimal.valueOf(cfg.getFeePercent()) : null)
                .transactionPrefix(cfg.getTransactionPrefix())
                .returnUrl(cfg.getReturnUrl())
                .webhookUrl(cfg.getWebhookUrl())
                .supportsRefunds(true)
                .supportsWebhooks(true)
                .connectTimeout(cfg.getConnectTimeout() != null ? cfg.getConnectTimeout() : 30000)
                .readTimeout(cfg.getReadTimeout() != null ? cfg.getReadTimeout() : 30000)
                .paymentParams(Map.of(
                        "capture", true,
                        "confirmation.type", "redirect"
                ))
                .build();
    }

    /**
     * Builds configuration for Qiwi payment provider.
     *
     * @return PaymentProviderConfig for Qiwi
     */
    private PaymentProviderConfig buildQiwiConfig() {
        PaymentConfig.ProvidersConfig.ProviderConfig cfg = paymentConfig.getProviders().getQiwi();

        log.debug(messageService.get("payment.gateway.factory.config.qiwi",
                cfg.getPaymentUrl(), cfg.getTransactionPrefix()));

        return PaymentProviderConfig.builder()
                .providerName("Qiwi")
                .providerCode("qiwi")
                .paymentUrl(cfg.getPaymentUrl())
                .refundUrl(cfg.getRefundUrl())
                .statusUrl(cfg.getStatusUrl())
                .apiKey(System.getenv("QIWI_API_KEY"))
                .authScheme(TOKEN_TYPE)
                .httpMethod(HttpMethod.PUT)
                .feePercent(cfg.getFeePercent() != null ? BigDecimal.valueOf(cfg.getFeePercent()) : null)
                .transactionPrefix(cfg.getTransactionPrefix())
                .returnUrl(cfg.getReturnUrl())
                .webhookUrl(cfg.getWebhookUrl())
                .supportsRefunds(true)
                .supportsWebhooks(true)
                .connectTimeout(cfg.getConnectTimeout() != null ? cfg.getConnectTimeout() : 30000)
                .readTimeout(cfg.getReadTimeout() != null ? cfg.getReadTimeout() : 30000)
                .paymentParams(Map.of(
                        "expirationDateTime", "P1D"
                ))
                .build();
    }

    /**
     * Builds configuration for PayPal payment provider.
     *
     * @return PaymentProviderConfig for PayPal
     */
    private PaymentProviderConfig buildPayPalConfig() {
        PaymentConfig.ProvidersConfig.ProviderConfig cfg = paymentConfig.getProviders().getPaypal();

        log.debug(messageService.get("payment.gateway.factory.config.paypal",
                cfg.getPaymentUrl(), cfg.getTransactionPrefix()));

        return PaymentProviderConfig.builder()
                .providerName("PayPal")
                .providerCode("paypal")
                .paymentUrl(cfg.getPaymentUrl())
                .refundUrl(cfg.getRefundUrl())
                .statusUrl(cfg.getStatusUrl())
                .apiKey(System.getenv("PAYPAL_CLIENT_ID"))
                .apiSecret(System.getenv("PAYPAL_CLIENT_SECRET"))
                .authScheme(TOKEN_TYPE)
                .httpMethod(HttpMethod.POST)
                .feePercent(cfg.getFeePercent() != null ? BigDecimal.valueOf(cfg.getFeePercent()) : null)
                .feeFixed(cfg.getFeeFixed() != null ? BigDecimal.valueOf(cfg.getFeeFixed()) : null)
                .transactionPrefix(cfg.getTransactionPrefix())
                .returnUrl(cfg.getReturnUrl())
                .webhookUrl(cfg.getWebhookUrl())
                .supportsRefunds(true)
                .supportsWebhooks(true)
                .connectTimeout(cfg.getConnectTimeout() != null ? cfg.getConnectTimeout() : 30000)
                .readTimeout(cfg.getReadTimeout() != null ? cfg.getReadTimeout() : 30000)
                .paymentParams(Map.of(
                        "intent", "CAPTURE"
                ))
                .build();
    }

    /**
     * Builds configuration for Stripe payment provider.
     *
     * @return PaymentProviderConfig for Stripe
     */
    private PaymentProviderConfig buildStripeConfig() {
        PaymentConfig.ProvidersConfig.ProviderConfig cfg = paymentConfig.getProviders().getStripe();

        log.debug(messageService.get("payment.gateway.factory.config.stripe",
                cfg.getPaymentUrl(), cfg.getTransactionPrefix()));

        return PaymentProviderConfig.builder()
                .providerName("Stripe")
                .providerCode("stripe")
                .paymentUrl(cfg.getPaymentUrl())
                .refundUrl(cfg.getRefundUrl())
                .statusUrl(cfg.getStatusUrl())
                .apiSecret(System.getenv("STRIPE_SECRET_KEY"))
                .authScheme(TOKEN_TYPE)
                .httpMethod(HttpMethod.POST)
                .feePercent(cfg.getFeePercent() != null ? BigDecimal.valueOf(cfg.getFeePercent()) : null)
                .feeFixed(cfg.getFeeFixed() != null ? BigDecimal.valueOf(cfg.getFeeFixed()) : null)
                .transactionPrefix(cfg.getTransactionPrefix())
                .returnUrl(cfg.getReturnUrl())
                .webhookUrl(cfg.getWebhookUrl())
                .supportsRecurring(true)
                .supportsRefunds(true)
                .supportsWebhooks(true)
                .connectTimeout(cfg.getConnectTimeout() != null ? cfg.getConnectTimeout() : 30000)
                .readTimeout(cfg.getReadTimeout() != null ? cfg.getReadTimeout() : 30000)
                .paymentParams(Map.of(
                        "capture_method", "automatic"
                ))
                .build();
    }

    /**
     * Adds or updates a gateway configuration at runtime.
     *
     * @param provider the payment provider
     * @param config   the provider configuration
     */
    public void registerGateway(PaymentProvider provider, PaymentProviderConfig config) {
        gateways.put(provider, new UniversalPaymentGateway(config, paymentConfig, messageService));
        log.info(messageService.get("payment.gateway.factory.registered", provider));
    }

    /**
     * Reloads the gateway configuration for a provider.
     *
     * @param provider the payment provider to reload
     */
    public void reloadGateway(PaymentProvider provider) {
        gateways.remove(provider);
        getGateway(provider);
        log.info(messageService.get("payment.gateway.factory.reloaded", provider));
    }
}