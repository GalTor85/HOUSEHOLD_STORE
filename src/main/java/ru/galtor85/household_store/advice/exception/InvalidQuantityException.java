package ru.galtor85.household_store.advice.exception;

public class InvalidQuantityException extends RuntimeException {
    private final Integer invalidQuantity;

    public InvalidQuantityException(Integer invalidQuantity) {
        super();
        this.invalidQuantity = invalidQuantity;
    }

    public Integer getInvalidQuantity() {
        return invalidQuantity;
    }
}