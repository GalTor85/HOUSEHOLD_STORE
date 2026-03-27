package ru.galtor85.household_store.entity.order;

import ru.galtor85.household_store.service.i18n.MessageService;

public enum SalesOrderType {
    RETAIL,
    WHOLESALE;

    /**
     * Получает локализованное название типа заказа
     */
    public String getLocalizedName(MessageService messageService) {
        return messageService.get("sales.order.type." + this.name());
    }

    /**
     * Получает локализованное название типа заказа с параметрами
     */
    public String getLocalizedName(MessageService messageService, Object... args) {
        return messageService.get("sales.order.type." + this.name(), args);
    }

    /**
     * Проверяет, является ли тип розничным
     */
    public boolean isRetail() {
        return this == RETAIL;
    }

    /**
     * Проверяет, является ли тип оптовым
     */
    public boolean isWholesale() {
        return this == WHOLESALE;
    }

    /**
     * Получает описание типа заказа
     */
    public String getDescription(MessageService messageService) {
        return messageService.get("sales.order.type.description." + this.name());
    }
}