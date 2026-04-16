package ru.galtor85.household_store.advice.exception.rollback;

import lombok.Getter;
import ru.galtor85.household_store.entity.order.OrderStatus;

/**
 * Exception thrown when rollback is not allowed for an order.
 */
@Getter
public class RollbackNotAllowedException extends RuntimeException {

    private static final String MESSAGE_FOR_STATUS = "Rollback not allowed for status: %s";
    private static final String MESSAGE_FOR_ORDER = "Rollback not allowed for order %d with status: %s";

    private final OrderStatus currentStatus;
    private final Long orderId;

    /**
     * Constructor for rollback by status.
     *
     * @param currentStatus current order status
     */
    public RollbackNotAllowedException(OrderStatus currentStatus) {
        super(String.format(MESSAGE_FOR_STATUS, currentStatus));
        this.currentStatus = currentStatus;
        this.orderId = null;
    }

    /**
     * Constructor for rollback by order.
     *
     * @param orderId order ID
     * @param currentStatus current order status
     */
    public RollbackNotAllowedException(Long orderId, OrderStatus currentStatus) {
        super(String.format(MESSAGE_FOR_ORDER, orderId, currentStatus));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }

    /**
     * Constructor with custom message.
     *
     * @param message custom message
     */
    public RollbackNotAllowedException(String message) {
        super(message);
        this.currentStatus = null;
        this.orderId = null;
    }

    /**
     * Constructor with custom message and cause.
     *
     * @param message custom message
     * @param cause the cause
     */
    public RollbackNotAllowedException(String message, Throwable cause) {
        super(message, cause);
        this.currentStatus = null;
        this.orderId = null;
    }
}