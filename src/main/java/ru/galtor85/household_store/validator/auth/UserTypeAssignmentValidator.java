package ru.galtor85.household_store.validator.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.validation.InvalidDateRangeException;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.time.LocalDateTime;

/**
 * Validator for user type assignment operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserTypeAssignmentValidator {

    private final LogMessageService logMsg;

    /**
     * Validates date range is valid (from before to).
     *
     * @param validFrom start date
     * @param validTo end date
     * @throws InvalidDateRangeException if from is after to
     */
    public void validateDateRange(LocalDateTime validFrom, LocalDateTime validTo) {
        if (validFrom != null && validTo != null && validFrom.isAfter(validTo)) {
            log.warn(logMsg.get("user-type.log.invalid.date.range", validFrom, validTo));
            throw new InvalidDateRangeException(validFrom, validTo);
        }
    }
}