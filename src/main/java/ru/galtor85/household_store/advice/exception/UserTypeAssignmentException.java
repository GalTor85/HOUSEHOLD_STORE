package ru.galtor85.household_store.advice.exception;

public class UserTypeAssignmentException extends RuntimeException {
    private final Long userId;
    private final String userType;

    public UserTypeAssignmentException(Long userId, String userType, String message) {
        super(message);
        this.userId = userId;
        this.userType = userType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserType() {
        return userType;
    }
}