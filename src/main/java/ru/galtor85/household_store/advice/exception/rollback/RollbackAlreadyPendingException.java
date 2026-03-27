package ru.galtor85.household_store.advice.exception.rollback;

import lombok.Getter;

@Getter
public class RollbackAlreadyPendingException extends RuntimeException {
    private final Long orderId;

    public RollbackAlreadyPendingException(Long orderId) {
        super("error.rollback.already.pending");
        this.orderId = orderId;
    }
}