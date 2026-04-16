package ru.galtor85.household_store.advice.exception.product;

import lombok.Getter;

@Getter
public class ProductNotFoundException extends RuntimeException {
    private final Long productId;

    public ProductNotFoundException(Long productId) {
        super();
        this.productId = productId;
    }

}