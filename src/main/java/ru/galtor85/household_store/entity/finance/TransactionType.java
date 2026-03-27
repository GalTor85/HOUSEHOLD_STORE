package ru.galtor85.household_store.entity.finance;

import ru.galtor85.household_store.service.i18n.MessageService;

public enum TransactionType {
    INCOME,
    EXPENSE,
    REFUND;

    /**
     * Получает локализованное название типа операции через MessageService
     */
    public String getLocalizedName(MessageService messageService) {
        return messageService.get("transaction.type." + this.name());
    }

    /**
     * Получает локализованное название типа операции с параметрами
     */
    public String getLocalizedName(MessageService messageService, Object... args) {
        return messageService.get("transaction.type." + this.name(), args);
    }

    /**
     * Получает описание типа операции
     */
    public String getDescription(MessageService messageService) {
        return messageService.get("transaction.type.description." + this.name());
    }

    /**
     * Получает цвет для отображения в UI (HEX)
     */
    public String getColor() {
        return switch (this) {
            case INCOME -> "#28a745";   // зеленый
            case EXPENSE -> "#dc3545";   // красный
            case REFUND -> "#ffc107";    // желтый
        };
    }

    /**
     * Получает иконку для отображения
     */
    public String getIcon() {
        return switch (this) {
            case INCOME -> "trending-up";
            case EXPENSE -> "trending-down";
            case REFUND -> "repeat";
        };
    }

    /**
     * Проверяет, является ли операция приходом
     */
    public boolean isIncome() {
        return this == INCOME;
    }

    /**
     * Проверяет, является ли операция расходом
     */
    public boolean isExpense() {
        return this == EXPENSE;
    }

    /**
     * Проверяет, является ли операция возвратом
     */
    public boolean isRefund() {
        return this == REFUND;
    }

    /**
     * Получает множитель для расчета (1 - приход, -1 - расход)
     */
    public int getMultiplier() {
        return switch (this) {
            case INCOME, REFUND -> 1;
            case EXPENSE -> -1;
        };
    }

    /**
     * Получает знак для отображения
     */
    public String getSign() {
        return switch (this) {
            case INCOME, REFUND -> "+";
            case EXPENSE -> "-";
        };
    }
}