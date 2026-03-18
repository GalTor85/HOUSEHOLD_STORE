package ru.galtor85.household_store.advice.exception;

import lombok.Getter;
import ru.galtor85.household_store.entity.OrderStatus;

@Getter
public class RollbackFinalStatusException extends RuntimeException {
    private final OrderStatus currentStatus;

    public RollbackFinalStatusException(OrderStatus currentStatus) {
        super("error.rollback.final.status");
        this.currentStatus = currentStatus;
    }
}