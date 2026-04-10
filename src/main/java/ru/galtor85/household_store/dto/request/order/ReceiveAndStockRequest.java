package ru.galtor85.household_store.dto.request.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.dto.common.ReceiveStockItem;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Receive purchase order and stock request",
        title = "Receive and Stock Request")
public class ReceiveAndStockRequest {

    @Schema(description = "Received at timestamp", example = "2024-03-17T10:30:00")
    private LocalDateTime receivedAt;

    @Schema(description = "Quality check passed", example = "true")
    private Boolean qualityCheck;

    @Schema(description = "Payment status", example = "PAID")
    private String paymentStatus;

    @NotNull(message = "{receive.validation.warehouse.id.empty}")
    @Schema(description = "Warehouse ID for receiving", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long warehouseId;

    @Schema(description = "Warehouse location", example = "Warehouse A, Section 3")
    private String warehouseLocation;

    @Valid
    @NotEmpty(message = "{receive.validation.items.empty}")
    @Schema(description = "Items to receive and stock", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ReceiveStockItem> items;

    @Schema(description = "Receiving notes", example = "All items in good condition")
    private String notes;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    /**
     * Gets total number of item types
     *
     * @return number of distinct items
     */
    @JsonIgnore
    @Schema(hidden = true)
    public int getTotalItems() {
        return items != null ? items.size() : 0;
    }

    /**
     * Gets total quantity of all items
     *
     * @return sum of all quantities
     */
    @JsonIgnore
    @Schema(hidden = true)
    public int getTotalQuantity() {
        return items != null ? items.stream()
                .mapToInt(ReceiveStockItem::getQuantity)
                .sum() : 0;
    }

    /**
     * Checks if quality check is performed
     *
     * @return true if quality check flag is present
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasQualityCheck() {
        return qualityCheck != null;
    }

    /**
     * Checks if payment status is provided
     *
     * @return true if payment status is not null and not empty
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasPaymentStatus() {
        return paymentStatus != null && !paymentStatus.trim().isEmpty();
    }

    /**
     * Checks if warehouse location is provided
     *
     * @return true if warehouse location is not null and not empty
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasWarehouseLocation() {
        return warehouseLocation != null && !warehouseLocation.trim().isEmpty();
    }

    /**
     * Checks if notes are provided
     *
     * @return true if notes is not null and not empty
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasNotes() {
        return notes != null && !notes.trim().isEmpty();
    }

    /**
     * Checks if received at timestamp is provided
     *
     * @return true if received at is not null
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasReceivedAt() {
        return receivedAt != null;
    }
}
