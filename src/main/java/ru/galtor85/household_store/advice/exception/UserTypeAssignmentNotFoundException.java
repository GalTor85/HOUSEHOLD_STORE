package ru.galtor85.household_store.advice.exception;

public class UserTypeAssignmentNotFoundException extends RuntimeException {
    private final Long userId;

    public UserTypeAssignmentNotFoundException(Long userId) {
        super();
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}