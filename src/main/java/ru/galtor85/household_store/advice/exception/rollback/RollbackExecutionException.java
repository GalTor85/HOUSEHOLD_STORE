package ru.galtor85.household_store.advice.exception.rollback;

import lombok.Getter;

@Getter
public class RollbackExecutionException extends RuntimeException {
    private final Long orderId;
    private final String errorDetails;

    public RollbackExecutionException(Long orderId, String errorDetails) {
        super("error.rollback.execution.failed");
        this.orderId = orderId;
        this.errorDetails = errorDetails;
    }
}