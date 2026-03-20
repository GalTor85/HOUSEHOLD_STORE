package ru.galtor85.household_store.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.InvalidDateRangeException;
import ru.galtor85.household_store.advice.exception.UserTypeAssignmentException;
import ru.galtor85.household_store.entity.UserTypeAssignment;
import ru.galtor85.household_store.repository.UserTypeAssignmentRepository;
import ru.galtor85.household_store.service.MessageService;

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