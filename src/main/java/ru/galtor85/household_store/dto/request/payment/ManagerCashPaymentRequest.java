package ru.galtor85.household_store.dto.request.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.finance.PaymentMethod;

import java.math.BigDecimal;

/**
 * Request DTO for manager cash payment from customer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Manager cash payment request from customer")
public class ManagerCashPaymentRequest {

    @NotNull(message = "{payment.validation.cash.register.id.required}")
    @Positive(message = "{payment.validation.cash.register.id.positive}")
    @Schema(description = "Cash register ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long cashRegisterId;

    @NotNull(message = "{payment.validation.payment.method.type.required}")
    @Schema(description = "Payment method type", example = "CASH", requiredMode = Schema.RequiredMode.REQUIRED)
    private PaymentMethod paymentMethod;

    @NotNull(message = "{payment.validation.amount.required}")
    @Positive(message = "{payment.validation.amount.positive}")
    @Schema(description = "Payment amount", example = "1500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "Order number", example = "SO-20240101-001")
    private String orderNumber;

    @Schema(description = "Invoice number", example = "INV-20240101-001")
    private String invoiceNumber;

    @Schema(description = "Payment description", example = "Customer cash payment for order")
    private String description;
}