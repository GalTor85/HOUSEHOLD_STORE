package ru.galtor85.household_store.advice.exception;

public class InsufficientCellCapacityException extends RuntimeException {
    private final Long cellId;
    private final String cellCode;
    private final int availableQuantity;
    private final int requestedQuantity;

    public InsufficientCellCapacityException(Long cellId, int availableQuantity, int requestedQuantity) {
        super();
        this.cellId = cellId;
        this.cellCode = null;
        this.availableQuantity = availableQuantity;
        this.requestedQuantity = requestedQuantity;
    }

    public Long getCellId() {
        return cellId;
    }

    public String getCellCode() {
        return cellCode;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }
}