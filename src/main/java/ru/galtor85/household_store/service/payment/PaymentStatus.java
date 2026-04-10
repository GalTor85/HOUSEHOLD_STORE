package ru.galtor85.household_store.service.payment;

/**
 * Payment status enumeration
 *
 * <p>Represents the possible states of a payment transaction throughout its lifecycle.
 * Each status provides utility methods for checking state transitions.</p>
 */
public enum PaymentStatus {

    /** Payment is pending - waiting for user action or processing */
    PENDING,

    /** Payment is being processed by the provider */
    PROCESSING,

    /** Payment has been successfully completed */
    COMPLETED,

    /** Payment has failed */
    FAILED,

    /** Payment has been cancelled by user or system */
    CANCELLED,

    /** Payment has been refunded */
    REFUNDED,

    /** Payment has expired (e.g., payment link expired) */
    EXPIRED,

    /** Payment is awaiting confirmation (e.g., bank transfer) */
    AWAITING_CONFIRMATION,

    /** Payment is on hold */
    ON_HOLD,

    /** Partial payment received */
    PARTIAL;

    /**
     * Checks if payment is final (cannot be changed)
     * Final states: COMPLETED, FAILED, CANCELLED, REFUNDED, EXPIRED
     *
     * @return true if status is final
     */
    public boolean isFinal() {
        return this == COMPLETED || this == FAILED ||
                this == CANCELLED || this == REFUNDED ||
                this == EXPIRED;
    }

    /**
     * Checks if payment is successful
     * Successful states: COMPLETED
     *
     * @return true if payment was successful
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    /**
     * Checks if payment can be refunded
     * Only completed payments can be refunded
     *
     * @return true if payment can be refunded
     */
    public boolean isRefundable() {
        return this == COMPLETED;
    }

    /**
     * Checks if payment can be cancelled
     * Pending, processing, and awaiting confirmation payments can be cancelled
     *
     * @return true if payment can be cancelled
     */
    public boolean isCancellable() {
        return this == PENDING || this == PROCESSING || this == AWAITING_CONFIRMATION;
    }

    /**
     * Gets localized status name
     *
     * @param messageService message service for localization
     * @return localized status name
     */
    public String getLocalizedName(ru.galtor85.household_store.service.i18n.MessageService messageService) {
        return messageService.get("payment.status." + this.name());
    }
}