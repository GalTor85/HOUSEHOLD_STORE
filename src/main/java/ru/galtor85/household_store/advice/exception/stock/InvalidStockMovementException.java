package ru.galtor85.household_store.advice.exception.stock;

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

    public String getReason() {
        return reason;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getFromCellId() {
        return fromCellId;
    }

    public Long getToCellId() {
        return toCellId;
    }
}