package ru.galtor85.household_store.advice.exception.stock;

import lombok.Getter;

/**
 * Exception thrown when product stock is not found
 */
@Getter
public class ProductStockNotFoundException extends RuntimeException {

    private final Long productId;
    private final Long warehouseId;

    public ProductStockNotFoundException(Long productId, Long warehouseId) {
        super();
        this.productId = productId;
        this.warehouseId = warehouseId;
    }

    public ProductStockNotFoundException(String message) {
        super(message);
        this.productId = null;
        this.warehouseId = null;
    }
}