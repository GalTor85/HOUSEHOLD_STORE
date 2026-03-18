package ru.galtor85.household_store.advice.exception;

import lombok.Getter;
import ru.galtor85.household_store.entity.OrderStatus;

@Getter
public class RollbackInvalidTransitionException extends RuntimeException {
    private final OrderStatus currentStatus;
    private final OrderStatus targetStatus;

    public RollbackInvalidTransitionException(OrderStatus currentStatus, OrderStatus targetStatus) {
        super("error.rollback.invalid.transition");
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }
}