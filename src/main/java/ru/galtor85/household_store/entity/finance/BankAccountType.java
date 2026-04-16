package ru.galtor85.household_store.entity.finance;

import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Bank account type enumeration
 */
@SuppressWarnings("unused")
public enum BankAccountType {

    /** Current/checking account */
    CHECKING,

    /** Savings account - for future use */
    SAVINGS,

    /** Foreign currency account - for future use */
    CURRENCY,
    /** Card account - for future use */
    CARD,
    /** Deposit account - for future use */
    DEPOSIT;

    /**
     * Gets localized account type name
     *
     * @param messageService message service
     * @return localized name
     */
    @SuppressWarnings("unused")
    public String getLocalizedName(MessageService messageService) {
        return messageService.get("bank.account.type." + this.name());
    }
}