package ru.galtor85.household_store.advice.exception.order;


import lombok.Getter;

@Getter
public class OrderNotFoundException extends RuntimeException {
    private final Long orderId;

    public OrderNotFoundException(Long orderId) {
        super();
        this.orderId = orderId;
    }

    public OrderNotFoundException(String orderNumber) {
        super("Order not found: " + orderNumber);
        this.orderId = null;
    }

    public OrderNotFoundException() {
        super();
        this.orderId = null;
    }


}