package ru.galtor85.household_store.advice.exception.auth;

public class InvalidCredentialsException extends RuntimeException {
    private final String identifier;

    public InvalidCredentialsException(String identifier) {
        super();
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}