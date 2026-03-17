package ru.galtor85.household_store.advice.exception;

public class CellAlreadyEmptyException extends RuntimeException {
    private final Long cellId;
    private final String cellCode;

    public CellAlreadyEmptyException(Long cellId) {
        super();
        this.cellId = cellId;
        this.cellCode = null;
    }

    public CellAlreadyEmptyException(String cellCode, Long warehouseId) {
        super();
        this.cellCode = cellCode;
        this.cellId = null;
    }

    public Long getCellId() {
        return cellId;
    }

    public String getCellCode() {
        return cellCode;
    }
}