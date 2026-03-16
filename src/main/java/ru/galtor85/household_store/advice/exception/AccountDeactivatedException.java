package ru.galtor85.household_store.advice.exception;

public class AccountDeactivatedException extends RuntimeException {
    private final Long userId;

    public AccountDeactivatedException(Long userId) {
        super();
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}