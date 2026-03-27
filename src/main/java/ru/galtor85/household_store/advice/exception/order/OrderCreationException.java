package ru.galtor85.household_store.advice.exception.order;

public class OrderCreationException extends RuntimeException {
    private final String orderNumber;

    public OrderCreationException(String orderNumber, String message) {
        super(message);
        this.orderNumber = orderNumber;
    }

    public String getOrderNumber() {
        return orderNumber;
    }
}