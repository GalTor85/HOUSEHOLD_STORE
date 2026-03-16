package ru.galtor85.household_store.advice.exception;

public class OrderItemNotFoundException extends RuntimeException {
    private final Long itemId;

    public OrderItemNotFoundException(Long itemId) {
        super();
        this.itemId = itemId;
    }

    public Long getItemId() {
        return itemId;
    }
}