package ru.galtor85.household_store.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
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
@Schema(description = "Receive purchase salesOrder and stock request",
        title = "Receive and Stock Request")
public class ReceiveAndStockRequest {

    @Schema(description = "Received at timestamp", example = "2024-03-17T10:30:00")
    private LocalDateTime receivedAt;

    @Schema(description = "Quality check passed", example = "true")
    private Boolean qualityCheck;

    @Schema(description = "Payment status", example = "PAID")
    private String paymentStatus;

    @NotNull(message = "{receive.validation.warehouse.id.empty}")
    @Schema(description = "Warehouse ID for receiving", example = "1", required = true)
    private Long warehouseId;

    @Column(name = "warehouse_location")
    private String warehouseLocation;

    @Valid
    @NotEmpty(message = "{receive.validation.items.empty}")
    @Schema(description = "Items to receive and stock", required = true)
    private List<ReceiveStockItem> items;

    @Schema(description = "Receiving notes", example = "All items in good condition")
    private String notes;

    /**
     * Получить общее количество позиций
     */
    public int getTotalItems() {
        return items != null ? items.size() : 0;
    }

    /**
     * Получить общее количество единиц товара
     */
    public int getTotalQuantity() {
        return items != null ? items.stream()
                .mapToInt(ReceiveStockItem::getQuantity)
                .sum() : 0;
    }
}
