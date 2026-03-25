package ru.galtor85.household_store.entity;

import ru.galtor85.household_store.service.MessageService;

public enum PaymentMethod {
    CASH,
    CARD,
    BANK_TRANSFER,
    ONLINE,
    CREDIT;

    /**
     * Получает локализованное название способа оплаты через MessageService
     */
    public String getLocalizedName(MessageService messageService) {
        return messageService.get("payment.method." + this.name());
    }

    /**
     * Получает локализованное название способа оплаты с параметрами
     */
    public String getLocalizedName(MessageService messageService, Object... args) {
        return messageService.get("payment.method." + this.name(), args);
    }

    /**
     * Получает описание способа оплаты
     */
    public String getDescription(MessageService messageService) {
        return messageService.get("payment.method.description." + this.name());
    }

    /**
     * Проверяет, является ли способ оплаты наличными
     */
    public boolean isCash() {
        return this == CASH;
    }

    /**
     * Проверяет, является ли способ оплаты безналичным
     */
    public boolean isNonCash() {
        return this == CARD || this == BANK_TRANSFER || this == ONLINE;
    }

    /**
     * Проверяет, требует ли способ оплаты дополнительной обработки
     */
    public boolean requiresProcessing() {
        return this == BANK_TRANSFER || this == ONLINE;
    }
}