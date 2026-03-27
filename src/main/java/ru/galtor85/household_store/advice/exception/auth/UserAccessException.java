package ru.galtor85.household_store.advice.exception.auth;

public class UserAccessException extends RuntimeException {
    public UserAccessException(String message) {
        super(message);
    }
}
