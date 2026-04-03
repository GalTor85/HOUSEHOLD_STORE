package ru.galtor85.household_store.advice.exception.order;

import lombok.Getter;

/**
 * Exception thrown when trying to reverse a purchase order with no received items
 */
@Getter
public class PurchaseOrderReverseException extends RuntimeException {

    private final Long orderId;
    private final String reason;

    public PurchaseOrderReverseException(Long orderId, String reason) {
        super();
        this.orderId = orderId;
        this.reason = reason;
    }

    public PurchaseOrderReverseException(String message) {
        super(message);
        this.orderId = null;
        this.reason = null;
    }
}