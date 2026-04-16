package ru.galtor85.household_store.advice.exception.stock;

import lombok.Getter;

@Getter
public class InvalidStockMovementException extends RuntimeException {
    private final String reason;
    private final Long productId;
    private final Long fromCellId;
    private final Long toCellId;

    public InvalidStockMovementException(String reason, Long productId, Long fromCellId, Long toCellId) {
        super();
        this.reason = reason;
        this.productId = productId;
        this.fromCellId = fromCellId;
        this.toCellId = toCellId;
    }

}