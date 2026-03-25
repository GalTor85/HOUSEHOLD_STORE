package ru.galtor85.household_store.advice.exception;

import lombok.Getter;

@Getter
public class SalesOrderNotFoundException extends RuntimeException {

    private final Long orderId;
    private final String orderNumber;

    public SalesOrderNotFoundException(Long orderId) {
        super();
        this.orderId = orderId;
        this.orderNumber = null;
    }

    public SalesOrderNotFoundException(String orderNumber) {
        super();
        this.orderNumber = orderNumber;
        this.orderId = null;
    }

    public SalesOrderNotFoundException(Long orderId, String message) {
        super(message);
        this.orderId = orderId;
        this.orderNumber = null;
    }
}