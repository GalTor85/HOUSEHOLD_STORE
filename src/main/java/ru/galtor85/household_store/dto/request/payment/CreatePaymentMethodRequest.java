package ru.galtor85.household_store.dto.request.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.payment.PaymentMethodType;
import ru.galtor85.household_store.entity.payment.PaymentProvider;
import ru.galtor85.household_store.entity.user.UserType;

import java.math.BigDecimal;
import java.util.Set;

import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_CURRENCY;

/**
 * Request DTO for creating a payment method (Manager/Admin only).
 *
 * <p>Supports all payment method types: CREDIT_CARD, BANK_ACCOUNT, ELECTRONIC,
 * CRYPTO, MOBILE_PAYMENT, CASH, INSTALLMENT.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentMethodRequest {

    // =========================================================================
    // BASIC SETTINGS (ALL TYPES)
    // =========================================================================

    /**
     * Display name of the payment method.
     */
    private String name;

    /**
     * Type of payment method.
     */
    private PaymentMethodType methodType;

    /**
     * User types that can use this payment method.
     */
    private Set<UserType> availableForUserTypes;

    /**
     * Payment provider.
     */
    private PaymentProvider provider;

    /**
     * Whether this payment method is active.
     */
    private Boolean active;

    /**
     * Display order (lower = higher priority).
     */
    private Integer sortOrder;

    /**
     * Currency code (ISO 4217).
     */
    private String currency;

    /**
     * Processing fee percentage.
     */
    private BigDecimal processingFee;

    /**
     * Minimum amount for using this payment method.
     */
    private BigDecimal minAmount;

    /**
     * Maximum amount for using this payment method.
     */
    private BigDecimal maxAmount;

    // =========================================================================
    // CREDIT CARD SETTINGS
    // =========================================================================

    /**
     * Credit card brand (VISA, Mastercard, MIR, etc.).
     */
    private String cardBrand;

    /**
     * Supported card types.
     */
    private Set<String> supportedCardTypes;

    /**
     * 3D Secure required.
     */
    private Boolean threeDSecureRequired;

    /**
     * Installments available for credit card.
     */
    private Boolean installmentsAvailable;

    /**
     * Maximum installment months.
     */
    private Integer maxInstallmentMonths;

    // =========================================================================
    // BANK ACCOUNT SETTINGS
    // =========================================================================

    /**
     * Bank name.
     */
    private String bankName;

    /**
     * Bank BIC/SWIFT code.
     */
    private String bankBic;

    /**
     * Bank account number.
     */
    private String bankAccountNumber;

    /**
     * Correspondent account number.
     */
    private String correspondentAccount;

    /**
     * Payment order processing time in days.
     */
    private Integer processingTimeDays;

    /**
     * Requires invoice upload.
     */
    private Boolean requiresInvoiceUpload;

    // =========================================================================
    // ELECTRONIC WALLET SETTINGS
    // =========================================================================

    /**
     * Wallet type (YooMoney, Qiwi, WebMoney, etc.).
     */
    private String walletType;

    /**
     * API endpoint URL.
     */
    private String apiUrl;

    /**
     * API key for authentication.
     */
    private String apiKey;

    /**
     * API secret for authentication.
     */
    private String apiSecret;

    /**
     * Merchant ID.
     */
    private String merchantId;

    /**
     * Webhook URL for payment notifications.
     */
    private String webhookUrl;

    /**
     * Success redirect URL.
     */
    private String successUrl;

    /**
     * Cancel redirect URL.
     */
    private String cancelUrl;

    // =========================================================================
    // CRYPTO SETTINGS
    // =========================================================================

    /**
     * Supported cryptocurrencies (BTC, ETH, USDT, etc.).
     */
    private Set<String> supportedCryptocurrencies;

    /**
     * Blockchain networks (ERC20, TRC20, BEP20, etc.).
     */
    private Set<String> blockchainNetworks;

    /**
     * Crypto wallet address.
     */
    private String walletAddress;

    /**
     * Minimum confirmation count.
     */
    private Integer minConfirmations;

    /**
     * Exchange rate update interval in minutes.
     */
    private Integer exchangeRateUpdateInterval;

    /**
     * Crypto payment processor (Binance Pay, Coinbase, etc.).
     */
    private String cryptoProcessor;

    // =========================================================================
    // MOBILE PAYMENT SETTINGS
    // =========================================================================

    /**
     * Mobile payment type (Apple Pay, Google Pay, Samsung Pay).
     */
    private String mobilePaymentType;

    /**
     * Merchant identifier for Apple Pay.
     */
    private String applePayMerchantId;

    /**
     * Google Pay merchant ID.
     */
    private String googlePayMerchantId;

    /**
     * Samsung Pay service ID.
     */
    private String samsungPayServiceId;

    /**
     * Payment gateway for mobile payments.
     */
    private String mobilePaymentGateway;

    // =========================================================================
    // CASH SETTINGS
    // =========================================================================

    /**
     * Cash payment type (point of sale, courier, pickup point).
     */
    private String cashType;

    /**
     * Allowed cash payment locations.
     */
    private Set<String> allowedLocations;

    /**
     * Maximum cash amount allowed.
     */
    private BigDecimal maxCashAmount;

    /**
     * Requires exact change.
     */
    private Boolean requiresExactChange;

    /**
     * Change allowed.
     */
    private Boolean changeAllowed;

    /**
     * Cash register ID for POS payments.
     */
    private Long cashRegisterId;

    // =========================================================================
    // INSTALLMENT SETTINGS
    // =========================================================================

    /**
     * Installment partner (Tinkoff, Yandex Split, etc.).
     */
    private String installmentPartner;

    /**
     * Available installment months options.
     */
    private Set<Integer> availableInstallmentMonths;

    /**
     * Interest rate for installments (0 = interest-free).
     */
    private BigDecimal interestRate;

    /**
     * First payment percentage (down payment).
     */
    private BigDecimal firstPaymentPercent;

    /**
     * Installment fee (one-time).
     */
    private BigDecimal installmentFee;

    /**
     * Requires bank card binding.
     */
    private Boolean requiresCardBinding;

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Checks if this is a credit card payment method.
     */
    public boolean isCreditCard() {
        return methodType == PaymentMethodType.CREDIT_CARD;
    }

    /**
     * Checks if this is a bank account payment method.
     */
    public boolean isBankAccount() {
        return methodType == PaymentMethodType.BANK_ACCOUNT;
    }

    /**
     * Checks if this is an electronic wallet payment method.
     */
    public boolean isElectronic() {
        return methodType == PaymentMethodType.ELECTRONIC;
    }

    /**
     * Checks if this is a cryptocurrency payment method.
     */
    public boolean isCrypto() {
        return methodType == PaymentMethodType.CRYPTO;
    }

    /**
     * Checks if this is a mobile payment method.
     */
    public boolean isMobilePayment() {
        return methodType == PaymentMethodType.MOBILE_PAYMENT;
    }

    /**
     * Checks if this is a cash payment method.
     */
    public boolean isCash() {
        return methodType == PaymentMethodType.CASH;
    }

    /**
     * Checks if this is an installment payment method.
     */
    public boolean isInstallment() {
        return methodType == PaymentMethodType.INSTALLMENT;
    }

    /**
     * Gets the normalized name (trimmed).
     *
     * @return trimmed name or null if name is null
     */
    public String getNormalizedName() {
        return name != null ? name.trim() : null;
    }

    /**
     * Gets the normalized currency (uppercase).
     *
     * @return uppercase currency code or default from TechnicalConstants
     */
    public String getNormalizedCurrency() {
        if (currency == null || currency.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        return currency.trim().toUpperCase();
    }
}