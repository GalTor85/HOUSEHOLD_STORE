package ru.galtor85.household_store.dto.request.stock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.dto.common.StockWriteOffItem;

import java.util.List;

import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_DESCRIPTION_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_WAREHOUSE_LOCATION_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_REASON_WRITEOFF_LENGTH;

/**
 * Request DTO for stock write-off.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Stock write-off request DTO", title = "Stock Write-Off Request")
public class StockWriteOffRequest {

    @NotEmpty(message = "{writeoff.validation.items.empty}")
    @Schema(description = "List of items to write off", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<StockWriteOffItem> items;

    @NotNull(message = "{writeoff.validation.reason.empty}")
    @Size(max = MAX_REASON_WRITEOFF_LENGTH, message = "{writeoff.validation.reason.max}")
    @Schema(description = "Reason for write-off", example = "DAMAGED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    @Size(max = MAX_DESCRIPTION_LENGTH, message = "{writeoff.validation.description.max}")
    @Schema(description = "Detailed description", example = "Damaged during transportation")
    private String description;

    @Size(max = MAX_WAREHOUSE_LOCATION_LENGTH, message = "{writeoff.validation.warehouse.location.max}")
    @Schema(description = "Warehouse location", example = "Warehouse A")
    private String warehouseLocation;

    @Schema(description = "Warehouse ID for write-off", example = "1")
    private Long warehouseId;

    @Schema(description = "Related order ID (if applicable)", example = "1")
    private Long relatedOrderId;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasWarehouseLocation() {
        return warehouseLocation != null && !warehouseLocation.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasWarehouseId() {
        return warehouseId != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasRelatedOrderId() {
        return relatedOrderId != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public int getTotalItems() {
        return items != null ? items.size() : 0;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public int getTotalQuantity() {
        if (items == null) return 0;
        return items.stream()
                .mapToInt(StockWriteOffItem::getQuantity)
                .sum();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedReason() {
        return reason != null ? reason.trim().toUpperCase() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedDescription() {
        return description != null ? description.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedWarehouseLocation() {
        return warehouseLocation != null ? warehouseLocation.trim() : null;
    }
}