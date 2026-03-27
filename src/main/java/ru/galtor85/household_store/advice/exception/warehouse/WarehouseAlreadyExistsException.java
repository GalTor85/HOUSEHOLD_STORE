package ru.galtor85.household_store.advice.exception.warehouse;

public class WarehouseAlreadyExistsException extends RuntimeException {
    private final String field;
    private final String value;

    public WarehouseAlreadyExistsException(String field, String value) {
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
