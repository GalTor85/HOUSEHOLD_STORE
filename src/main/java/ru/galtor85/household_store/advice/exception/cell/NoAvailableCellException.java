package ru.galtor85.household_store.advice.exception.cell;

import ru.galtor85.household_store.entity.warehouse.CellType;

public class NoAvailableCellException extends RuntimeException {
    private final Long warehouseId;
    private final CellType requiredType;

    public NoAvailableCellException(Long warehouseId, CellType requiredType) {
        super();
        this.warehouseId = warehouseId;
        this.requiredType = requiredType;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public CellType getRequiredType() {
        return requiredType;
    }
}