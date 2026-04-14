package ru.galtor85.household_store.advice.exception.order;

import lombok.Getter;

/**
 * Exception thrown when order cancellation is not allowed.
 */
@Getter
public class OrderCancellationNotAllowedException extends RuntimeException {

    private final Long orderId;

    public OrderCancellationNotAllowedException(String message) {
        super(message);
        this.orderId = null;
    }

    public OrderCancellationNotAllowedException(String message, Long orderId) {
        super(message);
        this.orderId = orderId;
    }
}