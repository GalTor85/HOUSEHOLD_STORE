package ru.galtor85.household_store.dto.request.finance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.finance.PaymentMethod;
import ru.galtor85.household_store.entity.finance.TransactionType;

import java.math.BigDecimal;

import static ru.galtor85.household_store.constants.TechnicalConstants.MIN_AMOUNT_STR;

/**
 * Request DTO for creating a cash transaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cash transaction request DTO")
public class CashTransactionRequest {

    // =========================================================================
    // REQUIRED FIELDS
    // =========================================================================

    @NotNull(message = "{cash.transaction.validation.register.id.empty}")
    @Schema(description = "Cash register ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long cashRegisterId;

    @NotNull(message = "{cash.transaction.validation.type.empty}")
    @Schema(description = "Transaction type", example = "INCOME", requiredMode = Schema.RequiredMode.REQUIRED)
    private TransactionType transactionType;

    @NotNull(message = "{cash.transaction.validation.amount.empty}")
    @DecimalMin(value = MIN_AMOUNT_STR, message = "{cash.transaction.validation.amount.min}")
    @Positive(message = "{cash.transaction.validation.amount.positive}")
    @Schema(description = "Transaction amount", example = "500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    // =========================================================================
    // OPTIONAL FIELDS
    // =========================================================================

    @Schema(description = "Invoice ID (for payment linking)", example = "1")
    private Long invoiceId;

    @Schema(description = "Sales order ID (for customer order payment)", example = "1")
    private Long salesOrderId;

    @Schema(description = "Purchase order ID (for supplier payment)", example = "1")
    private Long purchaseOrderId;

    @Schema(description = "Customer ID (for customer payments)", example = "1")
    private Long customerId;

    @Schema(description = "Original transaction ID (for refunds)", example = "1")
    private Long originalTransactionId;

    @Schema(description = "Currency code", example = "RUB")
    private String currency;

    @Schema(description = "Payment method", example = "CARD")
    private PaymentMethod paymentMethod;

    @Schema(description = "Description", example = "Customer payment")
    private String description;

    @Schema(description = "Additional notes", example = "Payment via terminal")
    private String notes;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedCurrency() {
        return currency != null ? currency.toUpperCase().trim() : null;
    }
}