package ru.galtor85.household_store.advice.exception.product;

public class ProductNotFoundException extends RuntimeException {
    private final Long productId;

    public ProductNotFoundException(Long productId) {
        super(); // Не передаем хардкодное сообщение
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}