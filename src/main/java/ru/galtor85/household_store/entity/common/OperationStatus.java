package ru.galtor85.household_store.entity.common;

import lombok.Getter;
import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Generic operation status enum for common operations.
 *
 * <p>This enum provides a standardized set of statuses that can be used
 * across different parts of the application where specific business enums
 * (like OrderStatus, InvoiceStatus) are not applicable.</p>
 *
 * <p>For logging and display purposes, use {@link #getLocalizedName(MessageService)}
 * to get localized status names.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Getter
public enum OperationStatus {

    /** Operation completed successfully */
    SUCCESS("SUCCESS", "operation.status.success"),

    /** Operation failed */
    FAILED("FAILED", "operation.status.failed"),

    /** Operation is pending */
    PENDING("PENDING", "operation.status.pending"),

    /** Operation is being processed */
    PROCESSING("PROCESSING", "operation.status.processing"),

    /** Operation completed */
    COMPLETED("COMPLETED", "operation.status.completed"),

    /** Operation cancelled */
    CANCELLED("CANCELLED", "operation.status.cancelled"),

    /** Operation refunded */
    REFUNDED("REFUNDED", "operation.status.refunded");

    private final String code;
    private final String messageKey;

    OperationStatus(String code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    /**
     * Gets the localized name of the status.
     *
     * @param messageService the message service for localization
     * @return localized status name
     */
    public String getLocalizedName(MessageService messageService) {
        return messageService.get(messageKey);
    }

    /**
     * Converts string code to OperationStatus enum.
     *
     * @param code the status code
     * @return matching OperationStatus
     * @throws IllegalArgumentException if code is invalid
     */
    public static OperationStatus fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Status code cannot be null");
        }
        for (OperationStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status code: " + code);
    }

    /**
     * Checks if the status is final (cannot be changed).
     *
     * @return true if status is SUCCESS, FAILED, CANCELLED, or REFUNDED
     */
    public boolean isFinal() {
        return this == SUCCESS || this == FAILED ||
                this == CANCELLED || this == REFUNDED;
    }

    /**
     * Checks if the status indicates success.
     *
     * @return true if status is SUCCESS or COMPLETED
     */
    public boolean isSuccess() {
        return this == SUCCESS || this == COMPLETED;
    }

    /**
     * Checks if the status indicates failure.
     *
     * @return true if status is FAILED or CANCELLED
     */
    public boolean isFailure() {
        return this == FAILED || this == CANCELLED;
    }
}