package ru.galtor85.household_store.dto.request.finance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.finance.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for creating a new invoice.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Invoice create request")
public class InvoiceCreateRequest {

    // =========================================================================
    // ORDER REFERENCES
    // =========================================================================

    @Schema(description = "Purchase order ID (for purchase invoices)")
    private Long purchaseOrderId;

    @Schema(description = "Sales order ID (for sales invoices)")
    private Long salesOrderId;

    // =========================================================================
    // FINANCIAL FIELDS
    // =========================================================================

    @NotNull(message = "{invoice.validation.amount.empty}")
    @Positive(message = "{invoice.validation.amount.positive}")
    @Schema(description = "Invoice amount", example = "1000.00")
    private BigDecimal amount;

    @Schema(description = "Currency code (ISO 4217)", example = "RUB")
    private String currency;

    @NotNull(message = "{invoice.validation.payment.method.empty}")
    @Schema(description = "Payment method", example = "CARD")
    private PaymentMethod paymentMethod;

    // =========================================================================
    // DATE FIELDS
    // =========================================================================

    @Schema(description = "Due date")
    private LocalDateTime dueDate;

    // =========================================================================
    // ADDITIONAL INFORMATION
    // =========================================================================

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Notes")
    private String notes;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasOrder() {
        return (purchaseOrderId != null && salesOrderId == null) ||
                (purchaseOrderId == null && salesOrderId != null);
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedCurrency() {
        return currency != null ? currency.trim().toUpperCase() : null;
    }
}