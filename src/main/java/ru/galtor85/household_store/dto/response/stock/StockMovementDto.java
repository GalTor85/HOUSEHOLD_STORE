package ru.galtor85.household_store.dto.response.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.stock.MovementType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Stock movement DTO", title = "Stock Movement")
public class StockMovementDto {

    @Schema(description = "Movement ID", example = "1")
    private Long id;

    @Schema(description = "Product ID", example = "123")
    private Long productId;

    @Schema(description = "Product name", example = "iPhone 13 Pro")
    private String productName;

    @Schema(description = "Product SKU", example = "IPHONE-13-PRO-128")
    private String productSku;

    @Schema(description = "Warehouse ID", example = "1")
    private Long warehouseId;

    @Schema(description = "Warehouse name", example = "Main Warehouse")
    private String warehouseName;

    @Schema(description = "From cell ID", example = "5")
    private Long fromCellId;

    @Schema(description = "From cell code", example = "A-01-01-01")
    private String fromCellCode;

    @Schema(description = "From warehouse name", example = "Main Warehouse")
    private String fromWarehouseName;

    @Schema(description = "To cell ID", example = "8")
    private Long toCellId;

    @Schema(description = "To cell code", example = "B-02-03-01")
    private String toCellCode;

    @Schema(description = "To warehouse name", example = "Main Warehouse")
    private String toWarehouseName;

    @Schema(description = "Quantity moved", example = "10")
    private Integer quantity;

    @Schema(description = "Movement type", example = "RECEIPT")
    private MovementType movementType;

    @Schema(description = "Localized movement type", example = "Поступление")
    private String localizedMovementType;

    @Schema(description = "Reference type (ORDER, PURCHASE, etc.)", example = "PURCHASE")
    private String referenceType;

    @Schema(description = "Reference ID", example = "15")
    private Long referenceId;

    @Schema(description = "Reference number", example = "PO-123456")
    private String referenceNumber;

    @Schema(description = "Performed by user ID", example = "1")
    private Long performedBy;

    @Schema(description = "Performed by user name", example = "admin@example.com")
    private String performedByName;

    @Schema(description = "Notes", example = "Damaged goods write-off")
    private String notes;

    @Schema(description = "Document number", example = "DOC-2024-001")
    private String documentNumber;

    @Schema(description = "Batch/Lot number", example = "BATCH-2024-001")
    private String batchNumber;

    @Schema(description = "Created at timestamp", example = "2024-03-17T10:30:00")
    private LocalDateTime createdAt;

    // ========== Вспомогательные методы ==========

    /**
     * Получить направление движения (IN/OUT/INTERNAL)
     */
    public String getDirection() {
        return switch (movementType) {
            case RECEIPT -> "IN";
            case SHIPMENT, WRITE_OFF -> "OUT";
            case TRANSFER, INVENTORY -> "INTERNAL";
            case RETURN -> "IN";
        };
    }

    /**
     * Проверка, является ли движение входящим
     */
    public boolean isIncoming() {
        return movementType == MovementType.RECEIPT || movementType == MovementType.RETURN;
    }

    /**
     * Проверка, является ли движение исходящим
     */
    public boolean isOutgoing() {
        return movementType == MovementType.SHIPMENT || movementType == MovementType.WRITE_OFF;
    }

    /**
     * Проверка, является ли движение внутренним перемещением
     */
    public boolean isInternal() {
        return movementType == MovementType.TRANSFER;
    }
}