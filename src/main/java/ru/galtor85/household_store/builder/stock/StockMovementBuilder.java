package ru.galtor85.household_store.builder.stock;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.stock.MovementType;
import ru.galtor85.household_store.entity.stock.StockMovement;

/**
 * Builder for stock movement entities.
 */
@Component
public class StockMovementBuilder {

    /**
     * Builds stock movement with all fields.
     *
     * @param productId product ID
     * @param fromCellId source cell ID
     * @param toCellId destination cell ID
     * @param warehouseId warehouse ID
     * @param quantity movement quantity
     * @param type movement type
     * @param referenceType reference type
     * @param referenceId reference ID
     * @param referenceNumber reference number
     * @param performedBy user ID who performed
     * @param notes movement notes
     * @param batchNumber batch number
     * @param documentNumber document number
     * @return stock movement entity
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