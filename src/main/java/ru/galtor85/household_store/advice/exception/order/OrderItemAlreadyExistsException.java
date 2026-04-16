package ru.galtor85.household_store.advice.exception.order;

import lombok.Getter;

@Getter
public class OrderItemAlreadyExistsException extends RuntimeException {
    private final Long productId;

    public OrderItemAlreadyExistsException(Long productId) {
        super();
        this.productId = productId;
    }

}