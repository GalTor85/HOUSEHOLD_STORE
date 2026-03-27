package ru.galtor85.household_store.builder.stock;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.stock.StockMovementDto;
import ru.galtor85.household_store.entity.stock.MovementType;
import ru.galtor85.household_store.entity.stock.StockMovement;

import java.time.LocalDateTime;

@Component
public class StockMovementDtoBuilder {
    /**
     * Создание DTO движения со всеми полями
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

    /**
     * Упрощенный метод для создания DTO из сущности (альтернативный подход)
     */
    public StockMovementDto fromEntity(StockMovement movement,
                                       String productName,
                                       String productSku,
                                       String fromCellCode,
                                       String fromWarehouseName,
                                       String toCellCode,
                                       String toWarehouseName,
                                       String warehouseName,
                                       String performedByName,
                                       String localizedType) {

        return StockMovementDto.builder()
                .id(movement.getId())
                .productId(movement.getProductId())
                .productName(productName)
                .productSku(productSku)
                .fromCellId(movement.getFromCellId())
                .fromCellCode(fromCellCode)
                .fromWarehouseName(fromWarehouseName)
                .toCellId(movement.getToCellId())
                .toCellCode(toCellCode)
                .toWarehouseName(toWarehouseName)
                .warehouseId(movement.getWarehouseId())
                .warehouseName(warehouseName)
                .quantity(movement.getQuantity())
                .movementType(movement.getMovementType())
                .localizedMovementType(localizedType)
                .referenceType(movement.getReferenceType())
                .referenceId(movement.getReferenceId())
                .referenceNumber(movement.getReferenceNumber())
                .performedBy(movement.getPerformedBy())
                .performedByName(performedByName)
                .notes(movement.getNotes())
                .batchNumber(movement.getBatchNumber())
                .documentNumber(movement.getDocumentNumber())
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
