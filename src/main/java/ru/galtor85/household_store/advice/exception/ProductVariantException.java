package ru.galtor85.household_store.advice.exception;

public class ProductVariantException extends RuntimeException {
    private final Long parentProductId;

    public ProductVariantException(Long parentProductId) {
        super(); // Не передаем хардкодное сообщение
        this.parentProductId = parentProductId;
    }

    public Long getParentProductId() {
        return parentProductId;
    }
}