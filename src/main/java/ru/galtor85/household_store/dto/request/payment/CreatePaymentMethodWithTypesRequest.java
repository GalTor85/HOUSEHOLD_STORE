package ru.galtor85.household_store.dto.request.payment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.payment.PaymentMethodType;
import ru.galtor85.household_store.entity.payment.PaymentProvider;
import ru.galtor85.household_store.entity.user.UserType;

import java.math.BigDecimal;
import java.util.Set;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Request DTO for creating a new payment method with user type assignments.
 *
 * <p>Managers use this DTO to create global payment methods and specify
 * which user types (RETAIL, WHOLESALE, VIP, PARTNER, EMPLOYEE) can use them.</p>
 *
 * @author G@LTor85
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for creating a payment method with user type assignments",
        title = "Create Payment Method With Types Request")
public class CreatePaymentMethodWithTypesRequest {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    private static final int DEFAULT_SORT_ORDER = 0;
    private static final String NONE_USER_TYPES = "none";

    // =========================================================================
    // REQUIRED FIELDS
    // =========================================================================

    @NotBlank(message = "{payment.validation.name.required}")
    @Size(max = MAX_PAYMENT_METHOD_NAME_LENGTH, message = "{payment.validation.name.max.length}")
    @Schema(description = "Display name for the payment method",
            example = "Sberbank Card",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull(message = "{payment.validation.method.type.required}")
    @Schema(description = "Type of payment method",
            example = "CREDIT_CARD",
            allowableValues = {"CREDIT_CARD", "BANK_ACCOUNT", "ELECTRONIC", "CASH"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private PaymentMethodType methodType;

    @NotNull(message = "{payment.validation.user.type.required}")
    @Schema(description = "User types that can use this payment method",
            example = "[\"RETAIL\", \"WHOLESALE\", \"VIP\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<UserType> availableForUserTypes;

    // =========================================================================
    // OPTIONAL FIELDS
    // =========================================================================

    @Schema(description = "Masked identifier for display (e.g., **** **** **** 1234)",
            example = "**** **** **** 1234")
    private String maskedIdentifier;

    @Schema(description = "Payment provider",
            example = "SBERBANK",
            allowableValues = {"SBERBANK", "YOOMONEY", "QIWI", "PAYPAL", "STRIPE", "VISA_MASTERCARD"})
    private PaymentProvider provider;

    @Schema(description = "Whether the payment method is active",
            example = "true",
            defaultValue = "true")
    @Builder.Default
    private Boolean active = true;

    @Schema(description = "Sort order for display (lower = higher priority)",
            example = "0",
            defaultValue = "0")
    @Builder.Default
    private Integer sortOrder = DEFAULT_SORT_ORDER;

    @Size(min = CURRENCY_CODE_LENGTH, max = CURRENCY_CODE_LENGTH, message = "{payment.validation.currency.invalid}")
    @Schema(description = "Currency code (ISO 4217)",
            example = "RUB",
            defaultValue = "RUB")
    @Builder.Default
    private String currency = DEFAULT_CURRENCY;

    @DecimalMin(value = "0.0", message = "{payment.validation.processing.fee.min}")
    @DecimalMax(value = "100.0", message = "{payment.validation.processing.fee.max}")
    @Schema(description = "Processing fee percentage",
            example = "0.00",
            defaultValue = "0.00")
    @Builder.Default
    private BigDecimal processingFee = BigDecimal.ZERO;

    // =========================================================================
    // CREDIT CARD SPECIFIC FIELDS
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

    // =========================================================================
    // BANK ACCOUNT SPECIFIC FIELDS
    // =========================================================================

    @Pattern(regexp = BANK_ACCOUNT_NUMBER_PATTERN, message = "{payment.validation.bank.account.number.invalid}")
    @Schema(description = "Bank account number (20 digits)",
            example = "40702810123456789012")
    private String bankAccountNumber;

    @Pattern(regexp = BANK_BIC_PATTERN, message = "{payment.validation.bank.bic.invalid}")
    @Schema(description = "Bank BIC (9 digits)",
            example = "044525225")
    private String bankBic;

    @Size(max = 200, message = "{payment.validation.bank.name.max}")
    @Schema(description = "Bank name",
            example = "Sberbank")
    private String bankName;

    // =========================================================================
    // ELECTRONIC WALLET SPECIFIC FIELDS
    // =========================================================================

    @Schema(description = "Electronic wallet ID",
            example = "wallet@yoomoney.ru")
    private String walletId;

    @Schema(description = "Phone number for wallet",
            example = "+79161234567")
    private String walletPhone;

    @Email(message = "{payment.validation.wallet.email.invalid}")
    @Schema(description = "Email for wallet",
            example = "user@example.com")
    private String walletEmail;

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isCreditCard() {
        return methodType == PaymentMethodType.CREDIT_CARD;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isBankAccount() {
        return methodType == PaymentMethodType.BANK_ACCOUNT;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isElectronic() {
        return methodType == PaymentMethodType.ELECTRONIC;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isCrypto() {
        return methodType == PaymentMethodType.CRYPTO;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isMobilePayment() {
        return methodType == PaymentMethodType.MOBILE_PAYMENT;
    }
}