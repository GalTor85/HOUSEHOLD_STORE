package ru.galtor85.household_store.advice.exception.order;

import lombok.Getter;

@Getter
public class OrderItemNotFoundException extends RuntimeException {
    private final Long itemId;

    public OrderItemNotFoundException(Long itemId) {
        super();
        this.itemId = itemId;
    }

}