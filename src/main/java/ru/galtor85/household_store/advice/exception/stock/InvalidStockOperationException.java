package ru.galtor85.household_store.advice.exception.stock;

import lombok.Getter;

@Getter
public class InvalidStockOperationException extends RuntimeException {
    private final int currentStock;
    private final int requestedChange;

    public InvalidStockOperationException(int currentStock, int requestedChange) {
        super();
        this.currentStock = currentStock;
        this.requestedChange = requestedChange;
    }

}