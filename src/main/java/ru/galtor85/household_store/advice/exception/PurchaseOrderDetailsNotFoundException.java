package ru.galtor85.household_store.advice.exception;

public class PurchaseOrderDetailsNotFoundException extends RuntimeException {
    private final Long orderId;

    public PurchaseOrderDetailsNotFoundException(Long orderId) {
        super();
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}