package ru.galtor85.household_store.dto.request.order;

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
@Schema(description = "Item to reverse from receipt")
public class ReverseReceiptItem {

    @NotNull(message = "Product ID is required")
    @Schema(description = "Product ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @Positive(message = "Quantity must be positive")
    @Schema(description = "Quantity to reverse", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @Schema(description = "Cell ID where product is stored", example = "10")
    private Long cellId;

    @Schema(description = "Batch number", example = "BATCH-20260331-ABC123")
    private String batchNumber;
}