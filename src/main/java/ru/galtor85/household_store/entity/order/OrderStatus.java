package ru.galtor85.household_store.entity.order;

import ru.galtor85.household_store.service.i18n.MessageService;

import java.util.List;

public enum OrderStatus {

    // Общие статусы
    PENDING(0),
    PAID(1),
    PROCESSING(2),
    DELIVERED(5),
    COMPLETED(6),
    CANCELLED(-1),

    // Статусы для продаж
    SHIPPED(3),
    REFUNDED(-2),
    RETURNED(-3),

    // Статусы для закупок
    PARTIALLY_RECEIVED(4);

    private final int step;

    OrderStatus(int step) {
        this.step = step;
    }

    public int getStep() {
        return step;
    }

    // =========================================================================
    // ЛОКАЛИЗАЦИЯ
    // =========================================================================

    /**
     * Получает локализованное название статуса
     */
    public String getLocalizedName(MessageService messageService) {
        return messageService.get("salesOrder.status." + this.name());
    }

    /**
     * Получает локализованное название статуса с параметрами
     */
    public String getLocalizedName(MessageService messageService, Object... args) {
        return messageService.get("salesOrder.status." + this.name(), args);
    }

    // =========================================================================
    // ПРОВЕРКА ПЕРЕХОДОВ ДЛЯ ЗАКУПОК
    // =========================================================================

    /**
     * Проверяет, допустим ли переход статуса для закупки
     */
    public boolean isValidTransitionForPurchase(OrderStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == PAID || newStatus == PROCESSING || newStatus == CANCELLED;
            case PAID -> newStatus == PROCESSING || newStatus == SHIPPED || newStatus == CANCELLED;
            case PROCESSING -> newStatus == SHIPPED || newStatus == DELIVERED || newStatus == CANCELLED;
            case SHIPPED -> newStatus == DELIVERED || newStatus == PARTIALLY_RECEIVED;
            case PARTIALLY_RECEIVED -> newStatus == DELIVERED || newStatus == COMPLETED;
            case DELIVERED -> newStatus == COMPLETED || newStatus == RETURNED;
            default -> false;
        };
    }

    /**
     * Проверяет, допустим ли переход статуса для закупки (с локализованным сообщением)
     */
    public boolean isValidTransitionForPurchase(OrderStatus newStatus, MessageService messageService) {
        boolean isValid = isValidTransitionForPurchase(newStatus);
        if (!isValid) {
            messageService.get("order.status.transition.invalid.purchase",
                    this.getLocalizedName(messageService),
                    newStatus.getLocalizedName(messageService));
        }
        return isValid;
    }

    // =========================================================================
    // ПРОВЕРКА ПЕРЕХОДОВ ДЛЯ ПРОДАЖ
    // =========================================================================

    /**
     * Проверяет, допустим ли переход статуса для продажи
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
     * Проверяет, допустим ли переход статуса для продажи (с локализованным сообщением)
     */
    public boolean isValidTransitionForSale(OrderStatus newStatus, MessageService messageService) {
        boolean isValid = isValidTransitionForSale(newStatus);
        if (!isValid) {
            messageService.get("order.status.transition.invalid.sale",
                    this.getLocalizedName(messageService),
                    newStatus.getLocalizedName(messageService));
        }
        return isValid;
    }

    // =========================================================================
    // ПРОВЕРКА ВОЗМОЖНОСТИ ОТКАТА (ROLLBACK)
    // =========================================================================

    /**
     * Проверяет, можно ли откатить статус
     */
    public boolean isRollbackAllowed() {
        return switch (this) {
            case PAID, PROCESSING, SHIPPED, DELIVERED, PARTIALLY_RECEIVED -> true;
            default -> false;
        };
    }

    /**
     * Проверяет, можно ли откатить статус (с локализованным сообщением)
     */
    public boolean isRollbackAllowed(MessageService messageService) {
        boolean allowed = isRollbackAllowed();
        if (!allowed) {
            messageService.get("order.status.rollback.not.allowed",
                    this.getLocalizedName(messageService));
        }
        return allowed;
    }

    /**
     * Получает статус для отката (на один шаг назад)
     */
    public OrderStatus getRollbackTarget() {
        return switch (this) {
            case PAID -> PENDING;
            case PROCESSING -> PAID;
            case SHIPPED -> PROCESSING;
            case DELIVERED -> SHIPPED;
            case PARTIALLY_RECEIVED -> PROCESSING;
            default -> this;
        };
    }

    /**
     * Проверяет, можно ли откатить статус для закупки
     */
    public boolean isRollbackAllowedForPurchase() {
        return switch (this) {
            case PAID, PROCESSING, SHIPPED, PARTIALLY_RECEIVED, DELIVERED -> true;
            default -> false;
        };
    }

    /**
     * Проверяет, можно ли откатить статус для закупки (с локализованным сообщением)
     */
    public boolean isRollbackAllowedForPurchase(MessageService messageService) {
        boolean allowed = isRollbackAllowedForPurchase();
        if (!allowed) {
            messageService.get("order.status.rollback.not.allowed.purchase",
                    this.getLocalizedName(messageService));
        }
        return allowed;
    }

    /**
     * Проверяет, можно ли откатить статус для продажи
     */
    public boolean isRollbackAllowedForSale() {
        return switch (this) {
            case PAID, PROCESSING, SHIPPED, DELIVERED -> true;
            default -> false;
        };
    }

    /**
     * Проверяет, можно ли откатить статус для продажи (с локализованным сообщением)
     */
    public boolean isRollbackAllowedForSale(MessageService messageService) {
        boolean allowed = isRollbackAllowedForSale();
        if (!allowed) {
            messageService.get("order.status.rollback.not.allowed.sale",
                    this.getLocalizedName(messageService));
        }
        return allowed;
    }

    /**
     * Получает целевой статус для отката в зависимости от типа заказа
     */
    public OrderStatus getRollbackTargetForPurchase() {
        return switch (this) {
            case PAID -> PENDING;
            case PROCESSING -> PAID;
            case SHIPPED -> PROCESSING;
            case PARTIALLY_RECEIVED -> PROCESSING;
            case DELIVERED -> SHIPPED;
            default -> this;
        };
    }

    public OrderStatus getRollbackTargetForSale() {
        return switch (this) {
            case PAID -> PENDING;
            case PROCESSING -> PAID;
            case SHIPPED -> PROCESSING;
            case DELIVERED -> SHIPPED;
            default -> this;
        };
    }

    // =========================================================================
    // ПРОВЕРКА ФИНАЛЬНЫХ СТАТУСОВ
    // =========================================================================

    /**
     * Проверяет, является ли статус финальным (нельзя изменить)
     */
    public boolean isFinal() {
        return switch (this) {
            case COMPLETED, CANCELLED, REFUNDED, RETURNED -> true;
            default -> false;
        };
    }

    /**
     * Проверяет, является ли статус финальным для закупки
     */
    public boolean isFinalForPurchase() {
        return switch (this) {
            case COMPLETED, CANCELLED, RETURNED -> true;
            default -> false;
        };
    }

    /**
     * Проверяет, является ли статус финальным для продажи
     */
    public boolean isFinalForSale() {
        return switch (this) {
            case COMPLETED, CANCELLED, REFUNDED, RETURNED -> true;
            default -> false;
        };
    }

    // =========================================================================
    // ПОЛУЧЕНИЕ СЛЕДУЮЩИХ СТАТУСОВ
    // =========================================================================

    /**
     * Получает список возможных следующих статусов для закупки
     */
    public List<OrderStatus> getNextStatusesForPurchase() {
        return switch (this) {
            case PENDING -> List.of(PAID, PROCESSING, CANCELLED);
            case PAID -> List.of(PROCESSING, SHIPPED, CANCELLED);
            case PROCESSING -> List.of(SHIPPED, DELIVERED, CANCELLED);
            case SHIPPED -> List.of(DELIVERED, PARTIALLY_RECEIVED);
            case PARTIALLY_RECEIVED -> List.of(DELIVERED, COMPLETED);
            case DELIVERED -> List.of(COMPLETED, RETURNED);
            default -> List.of();
        };
    }

    /**
     * Получает список возможных следующих статусов для продажи
     */
    public List<OrderStatus> getNextStatusesForSale() {
        return switch (this) {
            case PENDING -> List.of(PAID, CANCELLED);
            case PAID -> List.of(PROCESSING, CANCELLED, REFUNDED);
            case PROCESSING -> List.of(SHIPPED, CANCELLED);
            case SHIPPED -> List.of(DELIVERED);
            case DELIVERED -> List.of(COMPLETED, REFUNDED);
            case COMPLETED -> List.of(REFUNDED);
            default -> List.of();
        };
    }
}