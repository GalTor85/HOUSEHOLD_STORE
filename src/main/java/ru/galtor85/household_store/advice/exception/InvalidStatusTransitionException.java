package ru.galtor85.household_store.advice.exception;

import ru.galtor85.household_store.entity.OrderStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    private final OrderStatus currentStatus;
    private final OrderStatus newStatus;

    public InvalidStatusTransitionException(OrderStatus currentStatus, OrderStatus newStatus) {
        super();
        this.currentStatus = currentStatus;
        this.newStatus = newStatus;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }
}