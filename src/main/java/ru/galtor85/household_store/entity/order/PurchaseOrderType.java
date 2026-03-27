package ru.galtor85.household_store.entity.order;

import ru.galtor85.household_store.service.i18n.MessageService;

public enum PurchaseOrderType {
    PURCHASE;

    /**
     * Получает локализованное название типа заказа
     */
    public String getLocalizedName(MessageService messageService) {
        return messageService.get("purchase.order.type." + this.name());
    }

    /**
     * Получает локализованное название типа заказа с параметрами
     */
    public String getLocalizedName(MessageService messageService, Object... args) {
        return messageService.get("purchase.order.type." + this.name(), args);
    }

    /**
     * Получает описание типа заказа
     */
    public String getDescription(MessageService messageService) {
        return messageService.get("purchase.order.type.description." + this.name());
    }
}