package ru.galtor85.household_store.builder;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.MovementType;
import ru.galtor85.household_store.entity.StockMovement;

@Component
public class StockMovementBuilder {

    public StockMovement buildMovement(Long productId, Long fromCellId, Long toCellId,
                                       int quantity, MovementType type,
                                       String reference, Long performedBy) {
        return StockMovement.builder()
                .productId(productId)
                .fromCellId(fromCellId)
                .toCellId(toCellId)
                .quantity(quantity)
                .movementType(type)
                .referenceType(reference)
                .performedBy(performedBy)
                .notes("Auto-generated movement")
                .build();
    }
}