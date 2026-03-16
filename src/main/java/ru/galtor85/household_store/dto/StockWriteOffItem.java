package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Stock write-off item DTO", title = "Stock Write-Off Item")
public class StockWriteOffItem {

    @NotNull(message = "{writeoff.validation.product.id.empty}")
    @Positive(message = "{writeoff.validation.product.id.positive}")
    @Schema(description = "Product ID", example = "1", required = true)
    private Long productId;

    @NotNull(message = "{writeoff.validation.quantity.empty}")
    @Positive(message = "{writeoff.validation.quantity.positive}")
    @Schema(description = "Quantity to write off", example = "5", required = true)
    private Integer quantity;

    @Schema(description = "Write-off reason per item", example = "Expired")
    private String reason;

    @Schema(description = "Batch/lot number", example = "BATCH-2024-001")
    private String batchNumber;

    @Schema(description = "Expiration date (if applicable)", example = "2025-12-31")
    private String expirationDate;
}