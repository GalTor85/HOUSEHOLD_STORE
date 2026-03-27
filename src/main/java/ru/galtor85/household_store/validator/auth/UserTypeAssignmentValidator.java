package ru.galtor85.household_store.validator.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.validation.InvalidDateRangeException;
import ru.galtor85.household_store.advice.exception.user.UserTypeAssignmentException;
import ru.galtor85.household_store.entity.user.UserTypeAssignment;
import ru.galtor85.household_store.repository.user.UserTypeAssignmentRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserTypeAssignmentValidator {

    private final UserTypeAssignmentRepository assignmentRepository;
    private final MessageService messageService;

    public void validateDateRange(LocalDateTime validFrom, LocalDateTime validTo) {
        if (validFrom != null && validTo != null && validFrom.isAfter(validTo)) {
            log.warn(messageService.get("user-type.log.invalid.date.range", validFrom, validTo));
            throw new InvalidDateRangeException(validFrom, validTo);
        }
    }

    public UserTypeAssignment validateAssignmentExists(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-type.log.assignment.not.found", assignmentId));
                    return new UserTypeAssignmentException(null, null,
                            messageService.get("user-type.error.assignment.not.found", assignmentId));
                });
    }

    public void validateAssignmentNotActive(UserTypeAssignment assignment) {
        if (assignment.isActive()) {
            log.debug(messageService.get("user-type.log.already.active", assignment.getId()));
        }
    }
}