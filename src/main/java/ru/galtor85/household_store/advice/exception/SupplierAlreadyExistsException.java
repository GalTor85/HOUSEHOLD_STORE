package ru.galtor85.household_store.advice.exception;

public class SupplierAlreadyExistsException extends RuntimeException {
    private final String field;
    private final String value;

    public SupplierAlreadyExistsException(String field, String value) {
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