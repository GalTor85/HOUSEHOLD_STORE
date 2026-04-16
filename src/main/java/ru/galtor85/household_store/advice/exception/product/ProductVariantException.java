package ru.galtor85.household_store.advice.exception.product;

import lombok.Getter;

@Getter
public class ProductVariantException extends RuntimeException {
    private final Long parentProductId;

    public ProductVariantException(Long parentProductId) {
        super();
        this.parentProductId = parentProductId;
    }

}