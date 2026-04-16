package ru.galtor85.household_store.entity.finance;

import ru.galtor85.household_store.service.i18n.MessageService;

@SuppressWarnings("unused")
public enum PaymentMethod {
    CASH,
    CARD,
    BANK_TRANSFER,
    ONLINE,
    CREDIT;

    /**
     * Get the localized name of the payment method using the provided MessageService.
     */
    public String getLocalizedName(MessageService messageService) {
        return messageService.get("payment.method." + this.name());
    }
}