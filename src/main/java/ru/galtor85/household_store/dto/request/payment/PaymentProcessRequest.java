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

import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_TEXT_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MIN_AMOUNT_STR;

/**
 * Unified payment process request for all payment types.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Unified payment process request")
public class PaymentProcessRequest {

    private static final String TARGET_ORDER_PREFIX = "Order ";
    private static final String TARGET_INVOICE_PREFIX = "Invoice ";
    private static final String TARGET_PURCHASE_ORDER_PREFIX = "Purchase Order ";
    private static final String TARGET_REFUND_PREFIX = "Refund for transaction ";
    private static final String TARGET_UNKNOWN = "Unknown";

    @NotNull(message = "{payment.validation.payment.method.id.required}")
    @Positive(message = "{payment.validation.payment.method.id.positive}")
    @Schema(description = "Payment method ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long paymentMethodId;

    @Schema(description = "Order ID", example = "12345")
    private Long orderId;

    @Schema(description = "Invoice ID", example = "67890")
    private Long invoiceId;

    @Schema(description = "Purchase order ID", example = "11111")
    private Long purchaseOrderId;

    @Schema(description = "Original transaction ID for refunds", example = "99999")
    private Long originalTransactionId;

    @DecimalMin(value = MIN_AMOUNT_STR, message = "{payment.validation.amount.min}")
    @Schema(description = "Payment amount (defaults to full amount)", example = "1500.00")
    private BigDecimal amount;

    @Schema(description = "Customer ID", example = "100")
    private Long customerId;

    @Size(max = MAX_TEXT_LENGTH, message = "{payment.validation.refund.reason.max}")
    @Schema(description = "Refund reason", example = "Customer returned product")
    private String refundReason;

    @Schema(description = "Bank account ID", example = "50")
    private Long bankAccountId;

    @Schema(description = "Cash register ID", example = "10")
    private Long cashRegisterId;

    @Size(max = MAX_TEXT_LENGTH, message = "{payment.validation.description.max}")
    @Schema(description = "Payment description", example = "Order #12345 payment")
    private String description;

    @JsonIgnore
    public boolean isCustomerOrderPayment() {
        return orderId != null && customerId == null && purchaseOrderId == null;
    }

    @JsonIgnore
    public boolean isManagerSupplierBankPayment() {
        return purchaseOrderId != null && bankAccountId != null;
    }

    @JsonIgnore
    public boolean isManagerSupplierCashPayment() {
        return purchaseOrderId != null && cashRegisterId != null;
    }

    @JsonIgnore
    public boolean isRefund() {
        return originalTransactionId != null;
    }

    @JsonIgnore
    public boolean isInvoicePayment() {
        return invoiceId != null;
    }

    @JsonIgnore
    public String getPaymentTargetDescription() {
        if (orderId != null) return TARGET_ORDER_PREFIX + orderId;
        if (invoiceId != null) return TARGET_INVOICE_PREFIX + invoiceId;
        if (purchaseOrderId != null) return TARGET_PURCHASE_ORDER_PREFIX + purchaseOrderId;
        if (originalTransactionId != null) return TARGET_REFUND_PREFIX + originalTransactionId;
        return TARGET_UNKNOWN;
    }
}