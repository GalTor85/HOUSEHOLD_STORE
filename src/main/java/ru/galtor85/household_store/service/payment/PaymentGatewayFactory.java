package ru.galtor85.household_store.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.config.PaymentConfig;
import ru.galtor85.household_store.entity.payment.PaymentProvider;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.payment.gateway.PaymentProviderConfig;
import ru.galtor85.household_store.service.payment.gateway.UniversalPaymentGateway;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static ru.galtor85.household_store.constants.PaymentConstants.*;

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

    // =========================================================================
    // FIELDS
    // =========================================================================

    private final PaymentConfig paymentConfig;
    private final MessageService messageService;
    private final Map<PaymentProvider, UniversalPaymentGateway> gateways = new ConcurrentHashMap<>();

    // =========================================================================
    // PUBLIC METHODS
    // =========================================================================

    /**
     * Retrieves or creates a gateway for the specified payment provider.
     *
     * @param provider the payment provider
     * @return the UniversalPaymentGateway instance for the provider
     */
    public UniversalPaymentGateway getGateway(PaymentProvider provider) {
        if (provider == PaymentProvider.CASH_REGISTER) {
            PaymentProviderConfig cashConfig = buildCashRegisterConfig();
            return new UniversalPaymentGateway(cashConfig, paymentConfig, messageService);
        }
        return gateways.computeIfAbsent(provider, this::createGateway);
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

    // =========================================================================
    // PRIVATE METHODS - GATEWAY CREATION
    // =========================================================================

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

    // =========================================================================
    // PRIVATE METHODS - CONFIGURATION BUILDERS
    // =========================================================================

    /**
     * Builds provider configuration from application properties.
     *
     * @param provider the payment provider
     * @return PaymentProviderConfig with all settings from properties
     */
    private PaymentProviderConfig getConfigForProvider(PaymentProvider provider) {
        log.debug(messageService.get("payment.gateway.factory.building.config", provider));

        PaymentConfig.ProvidersConfig.ProviderConfig cfg = getProviderConfig(provider);

        // Use mock config for providers without real integration
        if (cfg == null) {
            log.warn(messageService.get("payment.gateway.factory.no.config", provider));
            return buildMockConfig(provider);
        }

        return PaymentProviderConfig.builder()
                .providerName(provider.name())
                .providerCode(provider.name().toLowerCase())
                .paymentUrl(cfg.getPaymentUrl())
                .refundUrl(cfg.getRefundUrl())
                .statusUrl(cfg.getStatusUrl())
                .apiKey(cfg.getApiKey())
                .apiSecret(cfg.getApiSecret())
                .feePercent(cfg.getFeePercent() != null ? BigDecimal.valueOf(cfg.getFeePercent()) : null)
                .feeFixed(cfg.getFeeFixed() != null ? BigDecimal.valueOf(cfg.getFeeFixed()) : null)
                .transactionPrefix(cfg.getTransactionPrefix())
                .returnUrl(cfg.getReturnUrl())
                .webhookUrl(cfg.getWebhookUrl())
                .connectTimeout(cfg.getConnectTimeout() != null ? cfg.getConnectTimeout() : DEFAULT_TIMEOUT)
                .readTimeout(cfg.getReadTimeout() != null ? cfg.getReadTimeout() : DEFAULT_TIMEOUT)
                .supportsRefunds(true)
                .build();
    }

    /**
     * Builds a mock configuration for payment providers that don't have real integration.
     * Used for testing and development purposes only.
     *
     * @param provider the payment provider to create mock config for
     * @return mock payment provider configuration
     */
    private PaymentProviderConfig buildMockConfig(PaymentProvider provider) {
        String prefix = provider.name().length() > MAX_TRANSACTION_PREFIX_LENGTH
                ? provider.name().substring(0, MAX_TRANSACTION_PREFIX_LENGTH)
                : provider.name();

        return PaymentProviderConfig.builder()
                .providerName(provider.name())
                .providerCode(provider.name().toLowerCase())
                .paymentUrl(MOCK_PAYMENT_URL)
                .feePercent(DEFAULT_FEE_PERCENT)
                .transactionPrefix(prefix)
                .supportsRefunds(true)
                .build();
    }

    /**
     * Builds configuration for cash register provider (offline payment).
     *
     * @return cash register payment provider configuration
     */
    private PaymentProviderConfig buildCashRegisterConfig() {
        return PaymentProviderConfig.builder()
                .providerName(CASH_REGISTER_NAME)
                .providerCode(CASH_PROVIDER_CODE)
                .feePercent(DEFAULT_FEE_PERCENT)
                .supportsRefunds(true)
                .build();
    }

    // =========================================================================
    // PRIVATE METHODS - PROVIDER CONFIGURATION MAPPING
    // =========================================================================

    /**
     * Retrieves provider-specific configuration from PaymentConfig.
     *
     * @param provider the payment provider
     * @return provider configuration or null if not found
     */
    private PaymentConfig.ProvidersConfig.ProviderConfig getProviderConfig(PaymentProvider provider) {
        return switch (provider) {
            case SBERBANK, VISA_MASTERCARD, MIR -> paymentConfig.getProviders().getSberbank();
            case YOOMONEY -> paymentConfig.getProviders().getYoomoney();
            case QIWI -> paymentConfig.getProviders().getQiwi();
            case PAYPAL -> paymentConfig.getProviders().getPaypal();
            case STRIPE -> paymentConfig.getProviders().getStripe();
            default -> null;
        };
    }
}