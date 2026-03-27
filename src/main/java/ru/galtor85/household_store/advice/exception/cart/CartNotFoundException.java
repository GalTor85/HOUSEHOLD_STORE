package ru.galtor85.household_store.advice.exception.cart;

public class CartNotFoundException extends RuntimeException {
    private final Long userId;

    public CartNotFoundException(Long userId) {
        super();
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}