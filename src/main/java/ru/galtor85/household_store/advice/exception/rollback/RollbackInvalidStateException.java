package ru.galtor85.household_store.advice.exception.rollback;

import lombok.Getter;
import ru.galtor85.household_store.entity.order.OrderStatus;

@Getter
public class RollbackInvalidStateException extends RuntimeException {
    private final Long orderId;
    private final OrderStatus currentStatus;
    private final String details;

    public RollbackInvalidStateException(Long orderId, OrderStatus currentStatus, String details) {
        super("error.rollback.invalid.state");
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.details = details;
    }
}