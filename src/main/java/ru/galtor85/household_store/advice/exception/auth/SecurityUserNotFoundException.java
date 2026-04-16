package ru.galtor85.household_store.advice.exception.auth;

import lombok.Getter;

@Getter
public class SecurityUserNotFoundException extends RuntimeException {
    private final Long userId;

    public SecurityUserNotFoundException(Long userId) {
        super();
        this.userId = userId;
    }

}