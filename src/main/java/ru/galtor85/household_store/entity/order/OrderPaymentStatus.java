package ru.galtor85.household_store.entity.order;

/**
 * Payment status for purchase orders and sales orders.
 * Represents the overall payment state of an entire order.
 */
public enum OrderPaymentStatus {

    /** Payment is pending, no payments received yet */
    PENDING,

    /** Order is fully paid */
    PAID,

    /** Order is partially paid */
    PARTIALLY_PAID,

    /** Payment is overdue */
    OVERDUE,

    /** Payment was refunded */
    REFUNDED,

    /** Payment was cancelled */
    CANCELLED;

    /**
     * Checks if the order is fully paid.
     *
     * @return true if status is PAID
     */
    public boolean isPaid() {
        return this == PAID;
    }


    /**
     * Checks if the order can be paid.
     *
     * @return true if status is PENDING or PARTIALLY_PAID
     */
    public boolean isPayable() {
        return this == PENDING || this == PARTIALLY_PAID;
    }

    /**
     * Checks if the order can be cancelled.
     *
     * @return true if status is PENDING
     */
    public boolean isCancellable() {
        return this == PENDING;
    }

    /**
     * Checks if the status is final (cannot be changed).
     *
     * @return true if status is PAID, REFUNDED, or CANCELLED
     */
    public boolean isFinal() {
        return this == PAID || this == REFUNDED || this == CANCELLED;
    }
}