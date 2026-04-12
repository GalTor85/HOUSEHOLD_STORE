package ru.galtor85.household_store.dto.request.payment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Unified Payment Process Request DTO.
 *
 * <p>Can be used for:</p>
 * <ul>
 *   <li>Customer paying for their order (INCOME)</li>
 *   <li>Customer paying an invoice (INCOME)</li>
 *   <li>Manager paying supplier from bank account (EXPENSE)</li>
 *   <li>Manager paying supplier from cash register (EXPENSE)</li>
 *   <li>Manager receiving cash from customer (INCOME)</li>
 *   <li>Manager processing refund (EXPENSE)</li>
 * </ul>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Unified payment process request for all payment types", title = "Payment Process Request")
public class PaymentProcessRequest {

    // =========================================================================
    // REQUIRED FIELDS (ALL CASES)
    // =========================================================================

    @NotNull(message = "{payment.validation.payment.method.id.required}")
    @Positive(message = "{payment.validation.payment.method.id.positive}")
    @Schema(description = "ID of the selected payment method",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long paymentMethodId;

    // =========================================================================
    // PAYMENT TARGET (one of these must be provided)
    // =========================================================================

    @Schema(description = "Order ID (for customer paying order or manager paying supplier)",
            example = "12345")
    private Long orderId;

    @Schema(description = "Invoice ID (for paying an invoice directly)",
            example = "67890")
    private Long invoiceId;

    @Schema(description = "Purchase order ID (for manager paying supplier)",
            example = "11111")
    private Long purchaseOrderId;

    @Schema(description = "Original transaction ID (for refunds)",
            example = "99999")
    private Long originalTransactionId;

    // =========================================================================
    // AMOUNT & CURRENCY
    // =========================================================================

    @Schema(description = "Payment amount (optional, defaults to full amount)",
            example = "1500.00")
    @DecimalMin(value = MIN_AMOUNT_STR, message = "{payment.validation.amount.min}")
    private BigDecimal amount;

    @Schema(description = "Currency code (ISO 4217)",
            example = "RUB",
            defaultValue = "RUB")
    @Size(min = CURRENCY_CODE_LENGTH, max = CURRENCY_CODE_LENGTH, message = "{payment.validation.currency.invalid}")
    private String currency;

    // =========================================================================
    // CUSTOMER DATA (for customer payments)
    // =========================================================================

    @Schema(description = "Customer ID (for manager receiving cash from customer)",
            example = "100")
    private Long customerId;

    @Schema(description = "Save payment method for future purchases",
            example = "false",
            defaultValue = "false")
    private Boolean savePaymentMethod;

    // =========================================================================
    // REFUND DATA
    // =========================================================================

    @Schema(description = "Refund reason (for refund operations)",
            example = "Customer returned product")
    @Size(max = MAX_TEXT_LENGTH, message = "{payment.validation.refund.reason.max}")
    private String refundReason;

    // =========================================================================
    // CREDIT CARD FIELDS
    // =========================================================================

    @Pattern(regexp = CREDIT_CARD_NUMBER_PATTERN, message = "{payment.validation.card.number.invalid}")
    @Schema(description = "Credit card number (16 digits)",
            example = "4111111111111111")
    private String cardNumber;

    @Pattern(regexp = CREDIT_CARD_EXPIRY_PATTERN, message = "{payment.validation.card.expiry.invalid}")
    @Schema(description = "Expiry date (MM/YY)",
            example = "12/25")
    private String expiryDate;

    @Pattern(regexp = CREDIT_CARD_CVV_PATTERN, message = "{payment.validation.card.cvv.invalid}")
    @Schema(description = "CVV/CVC code (3 or 4 digits)",
            example = "123")
    private String cvv;

    @Size(max = MAX_CARDHOLDER_NAME_LENGTH, message = "{payment.validation.card.holder.name.max}")
    @Schema(description = "Cardholder name",
            example = "IVAN IVANOV")
    private String cardHolderName;

    // =========================================================================
    // ELECTRONIC WALLET FIELDS
    // =========================================================================

    @Schema(description = "Electronic wallet ID (YooMoney, Qiwi, etc.)",
            example = "wallet@yoomoney.ru")
    private String walletId;

    @Schema(description = "Phone number linked to wallet",
            example = "+79161234567")
    private String walletPhone;

    @Email(message = "{payment.validation.wallet.email.invalid}")
    @Schema(description = "Email linked to wallet",
            example = "user@example.com")
    private String walletEmail;

    // =========================================================================
    // CRYPTO FIELDS
    // =========================================================================

    @Schema(description = "Cryptocurrency wallet address",
            example = "0x742d35Cc6634C0532925a3b844Bc9e7595f0b36f")
    private String cryptoWalletAddress;

    @Schema(description = "Blockchain network",
            example = "TRC20",
            allowableValues = {"ERC20", "TRC20", "BEP20", "SOL"})
    private String cryptoNetwork;

    // =========================================================================
    // MOBILE PAYMENT FIELDS
    // =========================================================================

    @Schema(description = "Mobile payment token (Apple Pay / Google Pay)",
            example = "tok_applepay_123456789")
    private String mobilePaymentToken;

    // =========================================================================
    // BANK ACCOUNT FIELDS (for manager payments)
    // =========================================================================

    @Schema(description = "Bank account ID (for manager paying from bank)",
            example = "50")
    private Long bankAccountId;

    @Schema(description = "Cash register ID (for manager paying from cash or receiving cash)",
            example = "10")
    private Long cashRegisterId;

    // =========================================================================
    // INSTALLMENT FIELDS
    // =========================================================================

    @Min(value = MIN_INSTALLMENT_MONTHS, message = "{payment.validation.installment.months.min}")
    @Max(value = MAX_INSTALLMENT_MONTHS, message = "{payment.validation.installment.months.max}")
    @Schema(description = "Number of installment months",
            example = "6")
    private Integer installmentMonths;

    // =========================================================================
    // CASH FIELDS
    // =========================================================================

    @Schema(description = "Cash pickup location",
            example = "PICKUP_POINT_MAIN")
    private String cashPickupLocation;

    // =========================================================================
    // COMMON OPTIONAL FIELDS
    // =========================================================================

    @Size(max = MAX_TEXT_LENGTH, message = "{payment.validation.description.max}")
    @Schema(description = "Payment description",
            example = "Order #12345 payment")
    private String description;

    // =========================================================================
    // HELPER METHODS - TYPE DETECTION
    // =========================================================================

    /**
     * Checks if this is a customer order payment.
     */
    @JsonIgnore
    public boolean isCustomerOrderPayment() {
        return orderId != null && customerId == null && purchaseOrderId == null;
    }

    /**
     * Checks if this is a manager supplier payment from bank.
     */
    @JsonIgnore
    public boolean isManagerSupplierBankPayment() {
        return purchaseOrderId != null && bankAccountId != null;
    }

    /**
     * Checks if this is a manager supplier payment from cash.
     */
    @JsonIgnore
    public boolean isManagerSupplierCashPayment() {
        return purchaseOrderId != null && cashRegisterId != null;
    }

    /**
     * Checks if this is a manager receiving cash from customer.
     */
    @JsonIgnore
    public boolean isManagerReceiveCash() {
        return customerId != null && cashRegisterId != null && invoiceId != null;
    }

    /**
     * Checks if this is a refund.
     */
    @JsonIgnore
    public boolean isRefund() {
        return originalTransactionId != null;
    }

    /**
     * Checks if this is an invoice payment.
     */
    @JsonIgnore
    public boolean isInvoicePayment() {
        return invoiceId != null;
    }

    /**
     * Checks if credit card data is provided.
     */
    @JsonIgnore
    public boolean hasCreditCardData() {
        return cardNumber != null || expiryDate != null || cvv != null;
    }

    /**
     * Checks if electronic wallet data is provided.
     */
    @JsonIgnore
    public boolean hasWalletData() {
        return walletId != null || walletPhone != null || walletEmail != null;
    }

    /**
     * Checks if crypto data is provided.
     */
    @JsonIgnore
    public boolean hasCryptoData() {
        return cryptoWalletAddress != null;
    }

    /**
     * Checks if mobile payment data is provided.
     */
    @JsonIgnore
    public boolean hasMobilePaymentData() {
        return mobilePaymentToken != null;
    }

    // =========================================================================
    // HELPER METHODS - NORMALIZATION
    // =========================================================================

    @JsonIgnore
    public String getNormalizedCardNumber() {
        return cardNumber != null ? cardNumber.replaceAll("\\s", "") : null;
    }

    @JsonIgnore
    public String getNormalizedWalletId() {
        return walletId != null ? walletId.trim().toLowerCase() : null;
    }

    @JsonIgnore
    public String getNormalizedWalletPhone() {
        return walletPhone != null ? walletPhone.replaceAll("\\D", "") : null;
    }

    @JsonIgnore
    public String getNormalizedCryptoWalletAddress() {
        return cryptoWalletAddress != null ? cryptoWalletAddress.trim() : null;
    }

    @JsonIgnore
    public String getEffectiveCurrency() {
        if (currency != null && !currency.isBlank()) {
            return currency.trim().toUpperCase();
        }
        return DEFAULT_CURRENCY_CODE;
    }

    @JsonIgnore
    public BigDecimal getEffectiveAmount(BigDecimal invoiceAmount) {
        return amount != null ? amount : invoiceAmount;
    }

    // =========================================================================
    // HELPER METHODS - VALIDATION
    // =========================================================================

    @JsonIgnore
    public boolean hasValidPaymentTarget() {
        return orderId != null || invoiceId != null || purchaseOrderId != null || originalTransactionId != null;
    }

    @JsonIgnore
    public String getPaymentTargetDescription() {
        if (orderId != null) return "Order " + orderId;
        if (invoiceId != null) return "Invoice " + invoiceId;
        if (purchaseOrderId != null) return "Purchase Order " + purchaseOrderId;
        if (originalTransactionId != null) return "Refund for transaction " + originalTransactionId;
        return "Unknown";
    }
}