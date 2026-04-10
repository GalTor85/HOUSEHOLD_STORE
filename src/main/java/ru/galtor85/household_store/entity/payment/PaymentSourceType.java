package ru.galtor85.household_store.entity.payment;

public enum PaymentSourceType {
    CASH("CASH"),
    CARD("CARD"),
    BANK_TRANSFER("BANK_TRANSFER"),
    ELECTRONIC_WALLET("ELECTRONIC_WALLET");

    private final String code;

    PaymentSourceType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PaymentSourceType fromCode(String code) {
        for (PaymentSourceType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown payment source: " + code);
    }
}