package ru.galtor85.household_store.advice.exception;

public class SystemInfoException extends RuntimeException {
    private final String infoType;

    public SystemInfoException(String infoType, String message) {
        super(message);
        this.infoType = infoType;
    }

    public SystemInfoException(String infoType, String message, Throwable cause) {
        super(message, cause);
        this.infoType = infoType;
    }

    public String getInfoType() {
        return infoType;
    }
}