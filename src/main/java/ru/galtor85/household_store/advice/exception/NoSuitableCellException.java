package ru.galtor85.household_store.advice.exception;

import ru.galtor85.household_store.entity.CellType;

public class NoSuitableCellException extends RuntimeException {
    private final Long warehouseId;
    private final CellType requiredType;
    private final Long productId;

    public NoSuitableCellException(Long warehouseId, CellType requiredType, Long productId) {
        super();
        this.warehouseId = warehouseId;
        this.requiredType = requiredType;
        this.productId = productId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public CellType getRequiredType() {
        return requiredType;
    }

    public Long getProductId() {
        return productId;
    }
}