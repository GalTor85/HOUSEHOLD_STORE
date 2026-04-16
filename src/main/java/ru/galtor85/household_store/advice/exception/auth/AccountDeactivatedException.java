package ru.galtor85.household_store.advice.exception.auth;

import lombok.Getter;

@Getter
public class AccountDeactivatedException extends RuntimeException {
    private final Long userId;

    public AccountDeactivatedException(Long userId) {
        super();
        this.userId = userId;
    }

}