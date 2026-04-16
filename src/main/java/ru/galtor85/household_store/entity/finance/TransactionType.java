package ru.galtor85.household_store.entity.finance;

import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Transaction type enumeration.
 */
public enum TransactionType {
    INCOME,
    EXPENSE,
    REFUND;

    private static final String COLOR_INCOME = "#28a745";
    private static final String COLOR_EXPENSE = "#dc3545";
    private static final String COLOR_REFUND = "#ffc107";
    private static final String ICON_INCOME = "trending-up";
    private static final String ICON_EXPENSE = "trending-down";
    private static final String ICON_REFUND = "repeat";
    private static final String SIGN_PLUS = "+";
    private static final String SIGN_MINUS = "-";

    /**
     * Returns localized transaction type name.
     */
    public String getLocalizedName(MessageService messageService) {
        return messageService.get("transaction.type." + name());
    }

    /**
     * Returns color for UI display.
     */
    public String getColor() {
        return switch (this) {
            case INCOME -> COLOR_INCOME;
            case EXPENSE -> COLOR_EXPENSE;
            case REFUND -> COLOR_REFUND;
        };
    }

    /**
     * Returns icon name for UI display.
     */
    public String getIcon() {
        return switch (this) {
            case INCOME -> ICON_INCOME;
            case EXPENSE -> ICON_EXPENSE;
            case REFUND -> ICON_REFUND;
        };
    }

    /**
     * Returns multiplier for balance calculation.
     */
    public int getMultiplier() {
        return switch (this) {
            case INCOME, REFUND -> 1;
            case EXPENSE -> -1;
        };
    }

    /**
     * Returns sign for display.
     */
    public String getSign() {
        return switch (this) {
            case INCOME, REFUND -> SIGN_PLUS;
            case EXPENSE -> SIGN_MINUS;
        };
    }
}