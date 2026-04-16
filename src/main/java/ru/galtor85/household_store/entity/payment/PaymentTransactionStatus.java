package ru.galtor85.household_store.entity.payment;

/**
 * Payment transaction status.
 */
public enum PaymentTransactionStatus {

    /**
     * Pending processing
     */
    PENDING,

    /**
     * Completed successfully
     */
    COMPLETED,

    /**
     * Failed
     */
    FAILED,

    /**
     * Refunded
     */
    REFUNDED,

    /**
     * Cancelled
     */
    CANCELLED

}