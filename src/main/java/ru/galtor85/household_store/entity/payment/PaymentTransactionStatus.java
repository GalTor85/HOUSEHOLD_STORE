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
     * In progress
     */
    PROCESSING,

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
    CANCELLED,

    /**
     * Pending verification (e.g., bank transfer)
     */
    PENDING_VERIFICATION
}