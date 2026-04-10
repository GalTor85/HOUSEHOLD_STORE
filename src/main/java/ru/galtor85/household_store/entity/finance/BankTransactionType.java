package ru.galtor85.household_store.entity.finance;

/**
 * Bank transaction type enumeration
 */
public enum BankTransactionType {
    /** Deposit to account (increase balance) */
    DEPOSIT,

    /** Withdrawal from account (decrease balance) */
    WITHDRAW,

    /** Transfer between accounts */
    TRANSFER
}