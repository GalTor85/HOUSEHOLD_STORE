package ru.galtor85.household_store.advice.exception;

import java.time.LocalDateTime;

public class InvalidDateRangeException extends RuntimeException {
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;

    public InvalidDateRangeException(LocalDateTime validFrom, LocalDateTime validTo) {
        super();
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }
}