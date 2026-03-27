package ru.galtor85.household_store.advice.exception.order;

public class InvalidOrderStatusException extends RuntimeException {
    private final String invalidStatus;

    public InvalidOrderStatusException(String invalidStatus) {
        super();
        this.invalidStatus = invalidStatus;
    }

    public String getInvalidStatus() {
        return invalidStatus;
    }
}