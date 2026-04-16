package ru.galtor85.household_store.advice.exception.auth;

import lombok.Getter;

@Getter
public class InvalidCredentialsException extends RuntimeException {
    private final String identifier;

    public InvalidCredentialsException(String identifier) {
        super();
        this.identifier = identifier;
    }

}