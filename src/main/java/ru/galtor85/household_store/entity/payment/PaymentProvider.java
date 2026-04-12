package ru.galtor85.household_store.entity.payment;

/**
 * Payment service providers
 */
public enum PaymentProvider {
    // Bank providers
    SBERBANK,
    VTB,
    ALFA_BANK,

    // Card providers
    VISA_MASTERCARD,
    MIR,

    // Electronic payment providers
    YOOMONEY,
    QIWI,
    WEBMONEY,
    PAYPAL,
    STRIPE,

    // Cryptocurrency
    CRYPTO,

    // Installment services
    SPLIT,
    YANDEX_SPLIT,

    //
    CASH_REGISTER
}