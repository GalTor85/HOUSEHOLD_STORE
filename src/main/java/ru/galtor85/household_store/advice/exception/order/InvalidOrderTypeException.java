package ru.galtor85.household_store.advice.exception.order;

import lombok.Getter;

@Getter
public class InvalidOrderTypeException extends RuntimeException {
    private final Long orderId;
    private final String expectedType;

    public InvalidOrderTypeException(Long orderId, String expectedType) {
        super();
        this.orderId = orderId;
        this.expectedType = expectedType;
    }

}