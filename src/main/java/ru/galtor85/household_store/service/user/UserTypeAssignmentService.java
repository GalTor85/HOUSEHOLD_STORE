package ru.galtor85.household_store.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.user.UserTypeAssignmentException;
import ru.galtor85.household_store.dto.response.user.UserTypeAssignmentDto;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.processor.assignment.AssignmentCreationProcessor;
import ru.galtor85.household_store.processor.assignment.AssignmentQueryProcessor;
import ru.galtor85.household_store.processor.assignment.PreviousAssignmentDeactivator;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.auth.UserTypeAssignmentValidator;

import java.time.LocalDateTime;

/**
 * Service for managing user type assignments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTypeAssignmentService {

    private final MessageService messageService;
    private final LogMessageService logMsg;
    private final UserTypeAssignmentValidator validator;
    private final PreviousAssignmentDeactivator deactivator;
    private final AssignmentCreationProcessor creationProcessor;
    private final AssignmentQueryProcessor queryProcessor;

    /**
     * Assigns a user type to a user.
     *
     * @param userId user ID
     * @param userType user type to assign
     * @param assignedBy who assigned the type
     * @param reason reason for assignment
     * @param validFrom validity start date
     * @param validTo validity end date
     * @return created assignment DTO
     */
    @Transactional
    public UserTypeAssignmentDto assignUserType(Long userId, UserType userType, String assignedBy,
                                                String reason, LocalDateTime validFrom, LocalDateTime validTo) {
        log.debug(logMsg.get("user-type.log.assignment.start", userId, userType, assignedBy));

        validator.validateDateRange(validFrom, validTo);

        try {
            deactivator.deactivatePrevious(userId);
            return creationProcessor.createAssignment(
                    userId, userType, assignedBy, reason, validFrom, validTo);
        } catch (Exception e) {
            log.error(logMsg.get("user-type.log.assignment.error", userId, userType, e.getMessage()), e);
            throw new UserTypeAssignmentException(userId, userType.name(),
                    messageService.get("user-type.error.assignment.failed", e.getMessage()));
        }
    }

    /**
     * Assigns a user type without validity dates.
     *
     * @param userId user ID
     * @param userType user type to assign
     * @param assignedBy who assigned the type
     * @param reason reason for assignment
     * @return created assignment DTO
     */
    @Transactional
    public UserTypeAssignmentDto assignUserType(Long userId, UserType userType, String assignedBy, String reason) {
        return assignUserType(userId, userType, assignedBy, reason, null, null);
    }

    /**
     * Gets current user type for a user.
     *
     * @param userId user ID
     * @return current assignment DTO or null
     */
    @Transactional(readOnly = true)
    public UserTypeAssignmentDto getCurrentUserType(Long userId) {
        return queryProcessor.getCurrentUserType(userId);
    }
}