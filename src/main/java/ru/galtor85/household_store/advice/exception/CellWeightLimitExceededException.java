package ru.galtor85.household_store.advice.exception;

public class CellWeightLimitExceededException extends RuntimeException {
    private final Long cellId;
    private final String cellCode;
    private final double maxWeight;
    private final double requestedWeight;

    public CellWeightLimitExceededException(Long cellId, double maxWeight, double requestedWeight) {
        super();
        this.cellId = cellId;
        this.cellCode = null;
        this.maxWeight = maxWeight;
        this.requestedWeight = requestedWeight;
    }

    public Long getCellId() {
        return cellId;
    }

    public String getCellCode() {
        return cellCode;
    }

    public double getMaxWeight() {
        return maxWeight;
    }

    public double getRequestedWeight() {
        return requestedWeight;
    }
}