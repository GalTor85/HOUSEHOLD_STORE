package ru.galtor85.household_store.advice.exception.stock;

import lombok.Getter;

@Getter
public class StockMovementNotFoundException extends RuntimeException {
    private final Long movementId;

    public StockMovementNotFoundException(Long movementId) {
        super();
        this.movementId = movementId;
    }

}