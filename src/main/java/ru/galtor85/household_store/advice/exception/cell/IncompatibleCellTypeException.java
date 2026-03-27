package ru.galtor85.household_store.advice.exception.cell;

import ru.galtor85.household_store.entity.warehouse.CellType;

public class IncompatibleCellTypeException extends RuntimeException {
    private final Long cellId;
    private final String cellCode;
    private final CellType cellType;
    private final String requiredType;

    public IncompatibleCellTypeException(Long cellId, CellType cellType, String requiredType) {
        super();
        this.cellId = cellId;
        this.cellCode = null;
        this.cellType = cellType;
        this.requiredType = requiredType;
    }

    public IncompatibleCellTypeException(String cellCode, Long warehouseId,
                                         CellType cellType, String requiredType) {
        super();
        this.cellCode = cellCode;
        this.cellType = cellType;
        this.requiredType = requiredType;
        this.cellId = null;
    }

    public Long getCellId() {
        return cellId;
    }

    public String getCellCode() {
        return cellCode;
    }

    public CellType getCellType() {
        return cellType;
    }

    public String getRequiredType() {
        return requiredType;
    }
}