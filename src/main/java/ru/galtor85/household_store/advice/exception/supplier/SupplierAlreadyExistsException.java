package ru.galtor85.household_store.advice.exception.supplier;

import lombok.Getter;

@Getter
public class SupplierAlreadyExistsException extends RuntimeException {
    private final String field;
    private final String value;

    public SupplierAlreadyExistsException(String field, String value) {
        super();
        this.field = field;
        this.value = value;
    }

}