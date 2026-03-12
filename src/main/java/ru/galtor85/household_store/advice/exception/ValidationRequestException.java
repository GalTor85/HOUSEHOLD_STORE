package ru.galtor85.household_store.advice.exception;

import lombok.Getter;

@Getter
public class ValidationRequestException extends RuntimeException {
    private final String principal;

    public ValidationRequestException(String message, String principal) {
        super(message);
        this.principal = principal;
    }

}
