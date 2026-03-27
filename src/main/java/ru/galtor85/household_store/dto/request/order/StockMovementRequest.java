package ru.galtor85.household_store.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.stock.MovementType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Stock movement request", title = "Stock Movement Request")
public class StockMovementRequest {

    @NotNull(message = "{movement.validation.product.id.empty}")
    @Schema(description = "Product ID", example = "123", required = true)
    private Long productId;

    @Schema(description = "Source cell ID (null for receipt)", example = "5")
    private Long fromCellId;

    @Schema(description = "Destination cell ID (null for shipment)", example = "8")
    private Long toCellId;

    @NotNull(message = "{movement.validation.quantity.empty}")
    @Min(value = 1, message = "{movement.validation.quantity.min}")
    @Schema(description = "Quantity to move", example = "10", required = true)
    private Integer quantity;

    @NotNull(message = "{movement.validation.type.empty}")
    @Schema(description = "Movement type", example = "TRANSFER", required = true)
    private MovementType movementType;

    @Schema(description = "Reference type", example = "ORDER")
    private String referenceType;

    @Schema(description = "Reference ID", example = "15")
    private Long referenceId;

    @Schema(description = "Notes", example = "Moving to better location")
    private String notes;

    @Schema(description = "Batch/Lot number", example = "BATCH-2024-001")
    private String batchNumber;
}