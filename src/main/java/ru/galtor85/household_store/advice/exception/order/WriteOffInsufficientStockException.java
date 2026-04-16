package ru.galtor85.household_store.advice.exception.order;

import lombok.Getter;

@Getter
public class WriteOffInsufficientStockException extends RuntimeException {
    private final Long productId;
    private final int availableStock;
    private final int requestedQuantity;

    public WriteOffInsufficientStockException(Long productId, int availableStock, int requestedQuantity) {
        super();
        this.productId = productId;
        this.availableStock = availableStock;
        this.requestedQuantity = requestedQuantity;
    }

}