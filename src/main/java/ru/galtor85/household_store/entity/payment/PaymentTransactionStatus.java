package ru.galtor85.household_store.entity.payment;

/**
 * Payment transaction status
 */
public enum PaymentTransactionStatus {
    PENDING,        // Pending processing
    PROCESSING,     // In progress
    COMPLETED,      // Completed
    FAILED,         // Failed
    REFUNDED,       // Refunded
    CANCELLED,      // Cancelled
    PENDING_VERIFICATION  // Pending verification
}