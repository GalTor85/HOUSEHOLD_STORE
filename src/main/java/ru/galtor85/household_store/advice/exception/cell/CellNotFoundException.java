package ru.galtor85.household_store.advice.exception.cell;

import lombok.Getter;

@Getter
public class CellNotFoundException extends RuntimeException {
    private final Long cellId;
    private final String cellCode;
    private final Long warehouseId;

    public CellNotFoundException(Long cellId) {
        super();
        this.cellId = cellId;
        this.cellCode = null;
        this.warehouseId = null;
    }

    public CellNotFoundException(String cellCode, Long warehouseId) {
        super();
        this.cellCode = cellCode;
        this.warehouseId = warehouseId;
        this.cellId = null;
    }

    public CellNotFoundException() {
        super();
        this.cellCode = null;
        this.warehouseId = null;
        this.cellId = null;
    }

}