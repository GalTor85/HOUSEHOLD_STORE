package ru.galtor85.household_store.advice.exception.cell;

import lombok.Getter;

@Getter
public class ProductAlreadyInWarehouseException extends RuntimeException {
    private final Long productId;
    private final Long warehouseId;
    private final String cellCode;

    public ProductAlreadyInWarehouseException(Long productId, Long warehouseId, String cellCode) {
        super();
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.cellCode = cellCode;
    }

}