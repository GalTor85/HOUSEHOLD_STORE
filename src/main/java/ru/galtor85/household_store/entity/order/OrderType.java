package ru.galtor85.household_store.entity.order;

import lombok.Getter;
import ru.galtor85.household_store.entity.finance.TransactionType;

/**
 * Order type enumeration for payment transactions.
 * Distinguishes between purchase orders (supplier payments) and sales orders (customer payments).
 */
@Getter
public enum OrderType {

    /** Purchase order - payment to supplier */
    PURCHASE("PURCHASE"),

    /** Sales order - payment from customer */
    SALES("SALES");

    private static final String PAYMENT = "payment";
    private static final String RECEIPT = "receipt";

    /**
     * -- GETTER --
     *  Gets the string code of the order type.
     */
    private final String code;

    OrderType(String code) {
        this.code = code;
    }

    /**
     * Returns the enum constant of the specified code.
     *
     * @param code the order type code
     * @return the matching OrderType
     * @throws IllegalArgumentException if no matching constant found
     */
    public static OrderType fromCode(String code) {
        for (OrderType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown order type: " + code);
    }

    /**
     * Returns the payment type for the given transaction type.
     *
     * @param transactionType the transaction type
     * @return payment type (PAYMENT or RECEIPT)
     */
    public String getPaymentType(TransactionType transactionType) {
        return transactionType == TransactionType.EXPENSE ? PAYMENT : RECEIPT;
    }
}