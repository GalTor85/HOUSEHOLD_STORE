package ru.galtor85.household_store.advice.exception.auth;

public class UserNotActiveException extends RuntimeException {
    public UserNotActiveException(String message) {
        super(message);
    }
}
