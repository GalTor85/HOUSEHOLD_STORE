package ru.galtor85.household_store.dto.request.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for transferring stock between warehouses or cells.
 *
 * @author G@LTor85
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Stock transfer request")
public class StockTransferRequest {

    @NotNull(message = "{stock.transfer.validation.product.id.required}")
    @Positive(message = "{stock.transfer.validation.product.id.positive}")
    @Schema(description = "Product ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @Schema(description = "Source warehouse ID (for warehouse transfer)")
    private Long fromWarehouseId;

    @Schema(description = "Source cell ID (optional)")
    private Long fromCellId;

    @Schema(description = "Source cell code (alternative to cellId)")
    private String fromCellCode;

    @NotNull(message = "{stock.transfer.validation.to.warehouse.id.required}")
    @Positive(message = "{stock.transfer.validation.to.warehouse.id.positive}")
    @Schema(description = "Destination warehouse ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long toWarehouseId;

    @Schema(description = "Destination cell ID (optional)")
    private Long toCellId;

    @Schema(description = "Destination cell code (alternative to cellId)")
    private String toCellCode;

    @NotNull(message = "{stock.transfer.validation.quantity.required}")
    @Positive(message = "{stock.transfer.validation.quantity.positive}")
    @Schema(description = "Quantity to transfer", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @Schema(description = "Batch/Lot number")
    private String batchNumber;

    @Schema(description = "Transfer reason", example = "Stock rebalancing")
    private String reason;

    @Schema(description = "Notes")
    private String notes;
}