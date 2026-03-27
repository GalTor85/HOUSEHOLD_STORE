package ru.galtor85.household_store.advice.exception.order;

import ru.galtor85.household_store.entity.order.OrderStatus;

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