package ru.galtor85.household_store.advice.exception.order;

import lombok.Getter;
import ru.galtor85.household_store.entity.order.OrderStatus;

/**
 * Exception thrown when a purchase order cannot be cancelled
 */
@Getter
public class PurchaseOrderCancellationException extends RuntimeException {

    private final Long orderId;
    private final OrderStatus currentStatus;
    private final String reason;

    /**
     * Constructor with order ID and status
     */
    public PurchaseOrderCancellationException(Long orderId, OrderStatus currentStatus) {
        super();
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.reason = null;
    }

    /**
     * Constructor with order ID, status and reason
     */
    public PurchaseOrderCancellationException(Long orderId, OrderStatus currentStatus, String reason) {
        super();
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.reason = reason;
    }

    /**
     * Constructor with custom message
     */
    public PurchaseOrderCancellationException(String message) {
        super(message);
        this.orderId = null;
        this.currentStatus = null;
        this.reason = null;
    }
}