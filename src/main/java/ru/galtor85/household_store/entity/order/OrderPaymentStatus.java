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
    CANCELLED
}