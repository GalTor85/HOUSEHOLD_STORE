package ru.galtor85.household_store.advice.exception.cell;

public class CellInactiveException extends RuntimeException {
    private final Long cellId;

    public CellInactiveException(Long cellId) {
        super();
        this.cellId = cellId;
    }

    public Long getCellId() {
        return cellId;
    }
}