package ru.galtor85.household_store.dto.request.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.stock.MovementType;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Request DTO for stock movement operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Stock movement request", title = "Stock Movement Request")
public class StockMovementRequest {

    @NotNull(message = "{movement.validation.product.id.empty}")
    @Schema(description = "Product ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @Schema(description = "Source cell ID (null for receipt)", example = "5")
    private Long fromCellId;

    @Schema(description = "Destination cell ID (null for shipment)", example = "8")
    private Long toCellId;

    @NotNull(message = "{movement.validation.quantity.empty}")
    @Min(value = MIN_MOVEMENT_QUANTITY, message = "{movement.validation.quantity.min}")
    @Schema(description = "Quantity to move", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @NotNull(message = "{movement.validation.type.empty}")
    @Schema(description = "Movement type", example = "TRANSFER", requiredMode = Schema.RequiredMode.REQUIRED)
    private MovementType movementType;

    @Size(max = MAX_REFERENCE_TYPE_LENGTH, message = "{movement.validation.reference.type.max}")
    @Schema(description = "Reference type", example = "ORDER")
    private String referenceType;

    @Schema(description = "Reference ID", example = "15")
    private Long referenceId;

    @Size(max = MAX_NOTES_LENGTH, message = "{movement.validation.notes.max}")
    @Schema(description = "Notes", example = "Moving to better location")
    private String notes;

    @Size(max = MAX_BATCH_NUMBER_LENGTH, message = "{movement.validation.batch.number.max}")
    @Schema(description = "Batch/Lot number", example = "BATCH-2024-001")
    private String batchNumber;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isReceipt() {
        return movementType == MovementType.RECEIPT;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isShipment() {
        return movementType == MovementType.SHIPMENT;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isTransfer() {
        return movementType == MovementType.TRANSFER;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isWriteOff() {
        return movementType == MovementType.WRITE_OFF;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isInventory() {
        return movementType == MovementType.INVENTORY;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isReturn() {
        return movementType == MovementType.RETURN;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasFromCell() {
        return fromCellId != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasToCell() {
        return toCellId != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasReference() {
        return referenceId != null && referenceType != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasBatchNumber() {
        return batchNumber != null && !batchNumber.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasNotes() {
        return notes != null && !notes.trim().isEmpty();
    }
}