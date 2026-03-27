package ru.galtor85.household_store.advice.exception.rollback;

import lombok.Getter;
import ru.galtor85.household_store.entity.order.OrderStatus;

import java.time.LocalDateTime;

@Getter
public class RollbackTimeoutException extends RuntimeException {
    private final Long orderId;
    private final OrderStatus currentStatus;
    private final LocalDateTime deliveredAt;

    public RollbackTimeoutException(Long orderId, OrderStatus currentStatus, LocalDateTime deliveredAt) {
        super("error.rollback.timeout");
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.deliveredAt = deliveredAt;
    }
}