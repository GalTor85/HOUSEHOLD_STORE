package ru.galtor85.household_store.entity.finance;

import ru.galtor85.household_store.service.i18n.MessageService;

public enum InvoiceStatus {
    PENDING,
    PAID,
    PARTIALLY_PAID,
    CANCELLED,
    REFUNDED;

    /**
     * Получает локализованное название статуса через MessageService
     */
    public String getLocalizedName(MessageService messageService) {
        return messageService.get("invoice.status." + this.name());
    }

    /**
     * Получает локализованное название статуса с параметрами
     */
    public String getLocalizedName(MessageService messageService, Object... args) {
        return messageService.get("invoice.status." + this.name(), args);
    }

    /**
     * Проверяет, является ли статус финальным
     */
    public boolean isFinal() {
        return this == PAID || this == CANCELLED || this == REFUNDED;
    }

    /**
     * Проверяет, можно ли оплатить счет
     */
    public boolean isPayable() {
        return this == PENDING || this == PARTIALLY_PAID;
    }

    /**
     * Проверяет, можно ли отменить счет
     */
    public boolean isCancellable() {
        return this == PENDING || this == PARTIALLY_PAID;
    }
}