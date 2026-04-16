package ru.galtor85.household_store.advice.exception.warehouse;

import lombok.Getter;

@Getter
public class WarehouseNotFoundException extends RuntimeException {
    private final Long warehouseId;
    private final String warehouseCode;

    public WarehouseNotFoundException(Long warehouseId) {
        super();
        this.warehouseId = warehouseId;
        this.warehouseCode = null;
    }

    public WarehouseNotFoundException(String warehouseCode) {
        super();
        this.warehouseCode = warehouseCode;
        this.warehouseId = null;
    }

    public WarehouseNotFoundException() {
        this.warehouseId = null;
        this.warehouseCode = null;
    }

}