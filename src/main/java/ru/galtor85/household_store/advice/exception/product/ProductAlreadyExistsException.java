package ru.galtor85.household_store.advice.exception.product;

import lombok.Getter;

@Getter
public class ProductAlreadyExistsException extends RuntimeException {
    private final String field;
    private final String value;

    public ProductAlreadyExistsException(String field, String value) {
        super();
        this.field = field;
        this.value = value;
    }

}