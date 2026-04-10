package ru.galtor85.household_store.dto.request.payment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static ru.galtor85.household_store.constants.TechnicalConstants.CURRENCY_CODE_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_DESCRIPTION_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MIN_AMOUNT_STR;

/**
 * Request DTO for processing a payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for processing a payment", title = "Payment Process Request")
public class PaymentProcessRequest {

    // =========================================================================
    // REQUIRED FIELDS
    // =========================================================================

    @NotNull(message = "{payment.validation.payment.method.id.required}")
    @Positive(message = "{payment.validation.payment.method.id.positive}")
    @Schema(description = "Payment method ID (bank account, credit card, etc.)",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long paymentMethodId;

    @NotNull(message = "{payment.validation.amount.required}")
    @DecimalMin(value = MIN_AMOUNT_STR, message = "{payment.validation.amount.min}")
    @Schema(description = "Payment amount",
            example = "5000.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @NotNull(message = "{payment.validation.currency.required}")
    @Size(min = CURRENCY_CODE_LENGTH, max = CURRENCY_CODE_LENGTH, message = "{payment.validation.currency.size}")
    @Schema(description = "Currency code (ISO 4217)",
            example = "RUB",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency;

    // =========================================================================
    // OPTIONAL FIELDS
    // =========================================================================

    @Schema(description = "Invoice ID (if paying an invoice)", example = "123")
    private Long invoiceId;

    @Schema(description = "Order ID (if paying an order)", example = "456")
    private Long orderId;

    @Schema(description = "Order type (PURCHASE, SALES)",
            example = "PURCHASE",
            allowableValues = {"PURCHASE", "SALES"})
    private String orderType;

    @Size(max = MAX_DESCRIPTION_LENGTH, message = "{payment.validation.description.max}")
    @Schema(description = "Payment description", example = "Payment for invoice INV-001")
    private String description;

    @Schema(description = "Return URL after successful payment", example = "https://yourdomain.com/payment/success")
    private String returnUrl;

    @Schema(description = "Cancel URL if payment is cancelled", example = "https://yourdomain.com/payment/cancel")
    private String cancelUrl;

    @Schema(description = "Webhook URL for payment notifications", example = "https://yourdomain.com/webhook/payment")
    private String webhookUrl;

    @Schema(description = "Payment metadata (JSON format)", example = "{\"order_number\": \"PO-12345\"}")
    private String metadata;

    @Schema(description = "Save payment method for future use", example = "true", defaultValue = "false")
    @Builder.Default
    private Boolean savePaymentMethod = false;

    @Schema(description = "Is recurring payment", example = "false", defaultValue = "false")
    @Builder.Default
    private Boolean recurring = false;

    @Schema(description = "Recurring payment interval in days (if recurring)", example = "30")
    private Integer recurringInterval;

    @Schema(description = "Number of recurring payments (if recurring)", example = "12")
    private Integer recurringCount;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    /**
     * Checks if payment is linked to an invoice.
     *
     * @return true if invoice ID is provided
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasInvoice() {
        return invoiceId != null;
    }

    /**
     * Checks if payment is linked to an order.
     *
     * @return true if order ID and order type are provided
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasOrder() {
        return orderId != null && orderType != null;
    }

    /**
     * Checks if payment is recurring.
     *
     * @return true if recurring
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isRecurring() {
        return Boolean.TRUE.equals(recurring);
    }

    /**
     * Checks if payment method should be saved.
     *
     * @return true if should save
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean shouldSavePaymentMethod() {
        return Boolean.TRUE.equals(savePaymentMethod);
    }

    /**
     * Gets normalized currency code (uppercase, trimmed).
     *
     * @return normalized currency or null
     */
    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedCurrency() {
        return currency != null ? currency.toUpperCase().trim() : null;
    }

    /**
     * Validates recurring payment parameters.
     *
     * @return true if valid
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isValidRecurring() {
        if (!isRecurring()) {
            return true;
        }
        return recurringInterval != null && recurringInterval > 0 &&
                recurringCount != null && recurringCount > 0;
    }

    /**
     * Checks if currency is provided.
     *
     * @return true if currency is not null and not blank
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasCurrency() {
        return currency != null && !currency.trim().isEmpty();
    }

    /**
     * Checks if description is provided.
     *
     * @return true if description is not null and not blank
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    /**
     * Checks if return URL is provided.
     *
     * @return true if return URL is not null and not blank
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasReturnUrl() {
        return returnUrl != null && !returnUrl.trim().isEmpty();
    }

    /**
     * Checks if cancel URL is provided.
     *
     * @return true if cancel URL is not null and not blank
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasCancelUrl() {
        return cancelUrl != null && !cancelUrl.trim().isEmpty();
    }

    /**
     * Checks if webhook URL is provided.
     *
     * @return true if webhook URL is not null and not blank
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasWebhookUrl() {
        return webhookUrl != null && !webhookUrl.trim().isEmpty();
    }

    /**
     * Checks if metadata is provided.
     *
     * @return true if metadata is not null and not blank
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasMetadata() {
        return metadata != null && !metadata.trim().isEmpty();
    }

    /**
     * Checks if recurring interval is valid.
     *
     * @return true if recurring interval is positive
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasValidRecurringInterval() {
        return recurringInterval != null && recurringInterval > 0;
    }

    /**
     * Checks if recurring count is valid.
     *
     * @return true if recurring count is positive
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasValidRecurringCount() {
        return recurringCount != null && recurringCount > 0;
    }
}