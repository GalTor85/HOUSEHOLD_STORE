package ru.galtor85.household_store.advice.exception;

import ru.galtor85.household_store.entity.OrderStatus;

public class CannotReceivePurchaseOrderException extends RuntimeException {
    private final OrderStatus currentStatus;

    public CannotReceivePurchaseOrderException(OrderStatus currentStatus) {
        super();
        this.currentStatus = currentStatus;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }
}