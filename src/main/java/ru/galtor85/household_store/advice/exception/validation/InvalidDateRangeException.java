package ru.galtor85.household_store.advice.exception.validation;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class InvalidDateRangeException extends RuntimeException {
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;

    public InvalidDateRangeException(LocalDateTime validFrom, LocalDateTime validTo) {
        super();
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

}