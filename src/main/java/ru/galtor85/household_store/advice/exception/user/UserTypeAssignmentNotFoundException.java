package ru.galtor85.household_store.advice.exception.user;

import lombok.Getter;

@Getter
public class UserTypeAssignmentNotFoundException extends RuntimeException {
    private final Long userId;

    public UserTypeAssignmentNotFoundException(Long userId) {
        super();
        this.userId = userId;
    }

}