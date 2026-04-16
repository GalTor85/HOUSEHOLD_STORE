package ru.galtor85.household_store.entity.cart;
/**
 * Status of a shopping cart.
 */
@SuppressWarnings("unused")
public enum CartStatus {
    ACTIVE,
    CHECKOUT,
    COMPLETED,
    ABANDONED,
    EXPIRED
}