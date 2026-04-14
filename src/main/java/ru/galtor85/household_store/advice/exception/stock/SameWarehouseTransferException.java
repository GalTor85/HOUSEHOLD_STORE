package ru.galtor85.household_store.advice.exception.stock;

import lombok.Getter;

/**
 * Exception thrown when attempting to transfer stock between the same warehouse.
 */
@Getter
public class SameWarehouseTransferException extends RuntimeException {

    private final Long warehouseId;

    public SameWarehouseTransferException(Long warehouseId) {
        super();
        this.warehouseId = warehouseId;
    }

}