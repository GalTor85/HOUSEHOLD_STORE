package ru.galtor85.household_store.advice.exception.order;

public class InvalidOrderTypeException extends RuntimeException {
    private final Long orderId;
    private final String expectedType;

    public InvalidOrderTypeException(Long orderId, String expectedType) {
        super();
        this.orderId = orderId;
        this.expectedType = expectedType;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getExpectedType() {
        return expectedType;
    }
}