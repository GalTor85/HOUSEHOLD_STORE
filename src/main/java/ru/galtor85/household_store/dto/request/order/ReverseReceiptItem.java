package ru.galtor85.household_store.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}