package ru.galtor85.household_store.advice.exception;

public class StockMovementNotFoundException extends RuntimeException {
    private final Long movementId;

    public StockMovementNotFoundException(Long movementId) {
        super();
        this.movementId = movementId;
    }

    public Long getMovementId() {
        return movementId;
    }
}