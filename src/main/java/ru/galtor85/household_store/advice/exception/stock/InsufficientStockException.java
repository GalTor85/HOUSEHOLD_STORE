package ru.galtor85.household_store.advice.exception.stock;

import lombok.Getter;

@Getter
public class InsufficientStockException extends RuntimeException {
    private final String productName;
    private final int availableStock;

    public InsufficientStockException(String productName, int availableStock) {
        super();
        this.productName = productName;
        this.availableStock = availableStock;
    }

}