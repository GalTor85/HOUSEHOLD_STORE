package ru.galtor85.household_store.entity.payment;

/**
 * Payment service providers
 */
public enum PaymentProvider {
    // Bank providers
    SBERBANK,

    // Card providers
    VISA_MASTERCARD,
    MIR,

    // Electronic payment providers
    YOOMONEY,
    QIWI,
    PAYPAL,
    STRIPE,

    // Cryptocurrency
    CRYPTO,

    //
    CASH_REGISTER
}