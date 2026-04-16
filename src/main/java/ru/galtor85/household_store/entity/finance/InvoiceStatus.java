package ru.galtor85.household_store.entity.finance;

import ru.galtor85.household_store.service.i18n.MessageService;

public enum InvoiceStatus {
    PENDING,
    PAID,
    PARTIALLY_PAID,
    CANCELLED,
    REFUNDED;

    /**
     * Gets the localized name of the invoice status using the provided MessageService.
     */
    public String getLocalizedName(MessageService messageService) {
        return messageService.get("invoice.status." + this.name());
    }
}