package ru.galtor85.household_store.builder.stock;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.stock.MovementType;
import ru.galtor85.household_store.entity.stock.StockMovement;

@Component
public class StockMovementBuilder {

    /**
     * Базовый метод создания движения
     */
    public StockMovement buildMovement(Long productId, Long fromCellId, Long toCellId,
                                       int quantity, MovementType type,
                                       String referenceType, Long performedBy) {

        return StockMovement.builder()
                .productId(productId)
                .fromCellId(fromCellId)
                .toCellId(toCellId)
                .quantity(quantity)
                .movementType(type)
                .referenceType(referenceType)
                .performedBy(performedBy)
                .build();
    }

    /**
     * Расширенный метод с номером документа
     */
    public StockMovement buildMovementWithDocument(Long productId, Long fromCellId, Long toCellId,
                                                   int quantity, MovementType type,
                                                   String referenceType, Long performedBy,
                                                   String documentNumber) {

        return StockMovement.builder()
                .productId(productId)
                .fromCellId(fromCellId)
                .toCellId(toCellId)
                .quantity(quantity)
                .movementType(type)
                .referenceType(referenceType)
                .performedBy(performedBy)
                .documentNumber(documentNumber)
                .build();
    }

    /**
     * Полный метод со всеми полями
     */
    public StockMovement buildFullMovement(Long productId, Long fromCellId, Long toCellId,
                                           Long warehouseId, int quantity, MovementType type,
                                           String referenceType, Long referenceId,
                                           String referenceNumber, Long performedBy,
                                           String notes, String batchNumber,
                                           String documentNumber) {

        return StockMovement.builder()
                .productId(productId)
                .fromCellId(fromCellId)
                .toCellId(toCellId)
                .warehouseId(warehouseId)
                .quantity(quantity)
                .movementType(type)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .performedBy(performedBy)
                .notes(notes)
                .batchNumber(batchNumber)
                .documentNumber(documentNumber)
                .build();
    }


}