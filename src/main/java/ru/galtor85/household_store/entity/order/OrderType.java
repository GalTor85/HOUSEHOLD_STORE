package ru.galtor85.household_store.entity.order;

/**
 * Order type enumeration for payment transactions.
 * Distinguishes between purchase orders (supplier payments) and sales orders (customer payments).
 */
public enum OrderType {

    /** Purchase order - payment to supplier */
    PURCHASE("PURCHASE"),

    /** Sales order - payment from customer */
    SALES("SALES");

    private final String code;

    OrderType(String code) {
        this.code = code;
    }

    /**
     * Gets the string code of the order type.
     *
     * @return order type code
     */
    public String getCode() {
        return code;
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
     * Checks if this is a purchase order type.
     *
     * @return true if PURCHASE
     */
    public boolean isPurchase() {
        return this == PURCHASE;
    }

    /**
     * Checks if this is a sales order type.
     *
     * @return true if SALES
     */
    public boolean isSales() {
        return this == SALES;
    }
}