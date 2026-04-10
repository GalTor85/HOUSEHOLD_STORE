package ru.galtor85.household_store.dto.request.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_BATCH_NUMBER_LENGTH;

/**
 * Request DTO for an item to reverse from receipt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Item to reverse from receipt")
public class ReverseReceiptItem {

    @NotNull(message = "{reverse.receipt.validation.product.id.required}")
    @Schema(description = "Product ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @Positive(message = "{reverse.receipt.validation.quantity.positive}")
    @Schema(description = "Quantity to reverse", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @Schema(description = "Cell ID where product is stored", example = "10")
    private Long cellId;

    @Size(max = MAX_BATCH_NUMBER_LENGTH, message = "{reverse.receipt.validation.batch.number.max}")
    @Schema(description = "Batch number", example = "BATCH-20260331-ABC123")
    private String batchNumber;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasCellId() {
        return cellId != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasBatchNumber() {
        return batchNumber != null && !batchNumber.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedBatchNumber() {
        return batchNumber != null ? batchNumber.trim().toUpperCase() : null;
    }
}