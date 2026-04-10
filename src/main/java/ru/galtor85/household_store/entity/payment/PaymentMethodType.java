package ru.galtor85.household_store.entity.payment;

/**
 * Payment method type enumeration
 */
public enum PaymentMethodType {
    BANK_ACCOUNT,   // Bank account
    CREDIT_CARD,    // Credit card
    ELECTRONIC,     // Electronic wallets (Qiwi, WebMoney, YooMoney)
    CRYPTO,         // Cryptocurrency
    MOBILE_PAYMENT, // Mobile payments (Apple Pay, Google Pay)
    INSTALLMENT     // Installment/Credit
}