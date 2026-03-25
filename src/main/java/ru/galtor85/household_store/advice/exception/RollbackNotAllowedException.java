package ru.galtor85.household_store.advice.exception;

import lombok.Getter;
import ru.galtor85.household_store.entity.OrderStatus;

@Getter
public class RollbackNotAllowedException extends RuntimeException {

    private final OrderStatus currentStatus;
    private final Long orderId;

    /**
     * Конструктор для отката по статусу
     */
    public RollbackNotAllowedException(OrderStatus currentStatus) {
        super(String.format("Rollback not allowed for status: %s", currentStatus));
        this.currentStatus = currentStatus;
        this.orderId = null;
    }

    /**
     * Конструктор для отката по заказу
     */
    public RollbackNotAllowedException(Long orderId, OrderStatus currentStatus) {
        super(String.format("Rollback not allowed for order %d with status: %s", orderId, currentStatus));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }

    /**
     * Конструктор с кастомным сообщением
     */
    public RollbackNotAllowedException(String message) {
        super(message);
        this.currentStatus = null;
        this.orderId = null;
    }

    /**
     * Конструктор с кастомным сообщением и причиной
     */
    public RollbackNotAllowedException(String message, Throwable cause) {
        super(message, cause);
        this.currentStatus = null;
        this.orderId = null;
    }

    /**
     * Получает локализованное сообщение (используется в GlobalExceptionHandler)
     */
    public String getLocalizedMessage(ru.galtor85.household_store.service.MessageService messageService) {
        if (currentStatus != null) {
            String localizedStatus = messageService.get("order.status." + currentStatus.name());
            if (orderId != null) {
                return messageService.get("rollback.not.allowed.for.order", orderId, localizedStatus);
            }
            return messageService.get("rollback.not.allowed.for.status", localizedStatus);
        }
        return getMessage();
    }
}