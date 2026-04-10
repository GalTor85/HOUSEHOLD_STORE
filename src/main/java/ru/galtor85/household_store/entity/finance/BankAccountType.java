package ru.galtor85.household_store.entity.finance;

import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Bank account type enumeration
 */
public enum BankAccountType {

    /** Current/checking account */
    CHECKING,

    /** Savings account */
    SAVINGS,

    /** Foreign currency account */
    CURRENCY,

    /** Card account */
    CARD,

    /** Deposit account */
    DEPOSIT;

    /**
     * Gets localized account type name
     *
     * @param messageService message service
     * @return localized name
     */
    public String getLocalizedName(MessageService messageService) {
        return messageService.get("bank.account.type." + this.name());
    }
}