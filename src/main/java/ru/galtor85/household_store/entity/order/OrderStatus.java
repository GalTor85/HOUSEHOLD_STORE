package ru.galtor85.household_store.entity.order;

import lombok.Getter;

/**
 * Order fulfillment status for both purchase and sales orders.
 */
@Getter
public enum OrderStatus {

    // Common statuses
    PENDING(0),
    PAID(1),
    PROCESSING(2),
    DELIVERED(5),
    COMPLETED(6),
    CANCELLED(-1),

    // Sales order statuses
    SHIPPED(3),
    REFUNDED(-2),
    RETURNED(-3),

    // Purchase order statuses
    PARTIALLY_RECEIVED(4);

    private final int step;

    OrderStatus(int step) {
        this.step = step;
    }

    /**
     * Checks if the transition is valid for a sales order.
     *
     * @param newStatus the target status
     * @return true if transition is allowed
     */
    public boolean isValidTransitionForSale(OrderStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == PAID || newStatus == CANCELLED;
            case PAID -> newStatus == PROCESSING || newStatus == CANCELLED || newStatus == REFUNDED;
            case PROCESSING -> newStatus == SHIPPED || newStatus == CANCELLED;
            case SHIPPED -> newStatus == DELIVERED;
            case DELIVERED -> newStatus == COMPLETED || newStatus == REFUNDED;
            case COMPLETED -> newStatus == REFUNDED;
            default -> false;
        };
    }

    /**
     * Checks if rollback is allowed for a sales order.
     *
     * @return true if rollback is allowed
     */
    public boolean isRollbackAllowedForSale() {
        return switch (this) {
            case PAID, PROCESSING, SHIPPED, DELIVERED -> true;
            default -> false;
        };
    }

    /**
     * Returns the target status for rollback on a sales order.
     *
     * @return the previous status in the workflow
     */
    public OrderStatus getRollbackTargetForSale() {
        return switch (this) {
            case PAID -> PENDING;
            case PROCESSING -> PAID;
            case SHIPPED -> PROCESSING;
            case DELIVERED -> SHIPPED;
            default -> this;
        };
    }
}