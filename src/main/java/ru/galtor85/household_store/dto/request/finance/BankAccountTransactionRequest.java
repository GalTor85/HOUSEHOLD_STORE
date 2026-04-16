package ru.galtor85.household_store.dto.request.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for bank account deposit or withdrawal operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for bank account transaction", title = "Bank Account Transaction Request")
public class BankAccountTransactionRequest {

    @NotNull(message = "{bank.transaction.validation.account.id.empty}")
    @Schema(description = "Bank account ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long accountId;

    @NotNull(message = "{bank.transaction.validation.amount.empty}")
    @Positive(message = "{bank.transaction.validation.amount.positive}")
    @Schema(description = "Transaction amount", example = "5000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "Transaction description", example = "Payment for invoice INV-001")
    private String description;
}