package ru.galtor85.household_store.advice.exception.order;

public class OrderItemAlreadyExistsException extends RuntimeException {
    private final Long productId;

    public OrderItemAlreadyExistsException(Long productId) {
        super();
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}