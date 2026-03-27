package ru.galtor85.household_store.advice.exception.product;

public class ProductAlreadyExistsException extends RuntimeException {
    private final String field;
    private final String value;

    public ProductAlreadyExistsException(String field, String value) {
        super();
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}