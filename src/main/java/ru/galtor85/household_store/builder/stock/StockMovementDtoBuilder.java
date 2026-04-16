package ru.galtor85.household_store.builder.stock;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.stock.StockMovementDto;
import ru.galtor85.household_store.entity.stock.MovementType;

import java.time.LocalDateTime;

/**
 * Builder for stock movement DTOs.
 */
@Component
public class StockMovementDtoBuilder {

    /**
     * Builds stock movement DTO with all fields.
     *
     * @param id movement ID
     * @param productId product ID
     * @param productName product name
     * @param productSku product SKU
     * @param fromCellId source cell ID
     * @param fromCellCode source cell code
     * @param fromWarehouseName source warehouse name
     * @param toCellId destination cell ID
     * @param toCellCode destination cell code
     * @param toWarehouseName destination warehouse name
     * @param warehouseId warehouse ID
     * @param warehouseName warehouse name
     * @param quantity movement quantity
     * @param movementType movement type
     * @param localizedMovementType localized movement type
     * @param referenceType reference type
     * @param referenceId reference ID
     * @param referenceNumber reference number
     * @param performedBy user ID who performed
     * @param performedByName user name who performed
     * @param notes movement notes
     * @param batchNumber batch number
     * @param documentNumber document number
     * @param createdAt creation timestamp
     * @return stock movement DTO
     */
    public StockMovementDto buildDto(
            Long id,
            Long productId,
            String productName,
            String productSku,
            Long fromCellId,
            String fromCellCode,
            String fromWarehouseName,
            Long toCellId,
            String toCellCode,
            String toWarehouseName,
            Long warehouseId,
            String warehouseName,
            Integer quantity,
            MovementType movementType,
            String localizedMovementType,
            String referenceType,
            Long referenceId,
            String referenceNumber,
            Long performedBy,
            String performedByName,
            String notes,
            String batchNumber,
            String documentNumber,
            LocalDateTime createdAt) {
        return StockMovementDto.builder()
                .id(id)
                .productId(productId)
                .productName(productName)
                .productSku(productSku)
                .fromCellId(fromCellId)
                .fromCellCode(fromCellCode)
                .fromWarehouseName(fromWarehouseName)
                .toCellId(toCellId)
                .toCellCode(toCellCode)
                .toWarehouseName(toWarehouseName)
                .warehouseId(warehouseId)
                .warehouseName(warehouseName)
                .quantity(quantity)
                .movementType(movementType)
                .localizedMovementType(localizedMovementType)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .performedBy(performedBy)
                .performedByName(performedByName)
                .notes(notes)
                .batchNumber(batchNumber)
                .documentNumber(documentNumber)
                .createdAt(createdAt)
                .build();
    }
}