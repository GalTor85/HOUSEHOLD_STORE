package ru.galtor85.household_store.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * DTO for stock write-off item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Stock write-off item DTO", title = "Stock Write-Off Item")
public class StockWriteOffItem {

    @NotNull(message = "{writeoff.validation.product.id.empty}")
    @Positive(message = "{writeoff.validation.product.id.positive}")
    @Schema(description = "Product ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @NotNull(message = "{writeoff.validation.quantity.empty}")
    @Positive(message = "{writeoff.validation.quantity.positive}")
    @Schema(description = "Quantity to write off", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @Size(max = MAX_REASON_LENGTH, message = "{writeoff.validation.reason.max}")
    @Schema(description = "Write-off reason per item", example = "Expired")
    private String reason;

    @Size(max = MAX_BATCH_NUMBER_LENGTH, message = "{writeoff.validation.batch.number.max}")
    @Schema(description = "Batch/lot number", example = "BATCH-2024-001")
    private String batchNumber;
}