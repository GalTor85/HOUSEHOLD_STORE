package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.PaymentMethod;
import ru.galtor85.household_store.entity.TransactionType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cash transaction request DTO")
public class CashTransactionRequest {

    // =========================================================================
    // ОБЯЗАТЕЛЬНЫЕ ПОЛЯ
    // =========================================================================

    @NotNull(message = "{cash.transaction.validation.register.id.empty}")
    @Schema(description = "Cash register ID", example = "1", required = true)
    private Long cashRegisterId;

    @NotNull(message = "{cash.transaction.validation.type.empty}")
    @Schema(description = "Transaction type", example = "INCOME", required = true)
    private TransactionType transactionType;

    @NotNull(message = "{cash.transaction.validation.amount.empty}")
    @DecimalMin(value = "0.01", message = "{cash.transaction.validation.amount.min}")
    @Positive(message = "{cash.transaction.validation.amount.positive}")
    @Schema(description = "Transaction amount", example = "500.00", required = true)
    private BigDecimal amount;

    // =========================================================================
    // ОПЦИОНАЛЬНЫЕ ПОЛЯ
    // =========================================================================

    @Schema(description = "Invoice ID (for payment linking)", example = "1")
    private Long invoiceId;

    @Schema(description = "Currency code", example = "RUB", defaultValue = "RUB")
    @Builder.Default
    private String currency = "RUB";

    @Schema(description = "Payment method", example = "CARD")
    private PaymentMethod paymentMethod;

    @Schema(description = "Description", example = "Customer payment")
    private String description;

    @Schema(description = "Additional notes", example = "Payment via terminal")
    private String notes;

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    public String getNormalizedCurrency() {
        return currency != null ? currency.toUpperCase().trim() : "RUB";
    }

    public boolean hasPaymentMethod() {
        return paymentMethod != null;
    }

    public boolean hasNotes() {
        return notes != null && !notes.trim().isEmpty();
    }

    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    public boolean isIncome() {
        return transactionType == TransactionType.INCOME;
    }

    public boolean isExpense() {
        return transactionType == TransactionType.EXPENSE;
    }

    public boolean isRefund() {
        return transactionType == TransactionType.REFUND;
    }

    public boolean hasInvoice() {
        return invoiceId != null;
    }
}