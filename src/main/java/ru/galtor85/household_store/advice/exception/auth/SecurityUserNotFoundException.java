package ru.galtor85.household_store.advice.exception.auth;

public class SecurityUserNotFoundException extends RuntimeException {
    private final Long userId;

    public SecurityUserNotFoundException(Long userId) {
        super();
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}