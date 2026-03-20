package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.UserTypeAssignmentException;
import ru.galtor85.household_store.dto.UserTypeAssignmentDto;
import ru.galtor85.household_store.entity.UserType;
import ru.galtor85.household_store.processor.*;
import ru.galtor85.household_store.validator.UserTypeAssignmentValidator;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserTypeAssignmentService {

    private final MessageService messageService;

    // Валидаторы
    private final UserTypeAssignmentValidator validator;

    // Процессоры
    private final PreviousAssignmentDeactivator deactivator;
    private final AssignmentCreationProcessor creationProcessor;
    private final AssignmentQueryProcessor queryProcessor;
    private final AssignmentReactivationProcessor reactivationProcessor;

    // ========== НАЗНАЧЕНИЕ ТИПА ==========

    @Transactional
    public UserTypeAssignmentDto assignUserType(Long userId, UserType userType, String assignedBy,
                                                String reason, LocalDateTime validFrom, LocalDateTime validTo) {
        log.debug(messageService.get("user-type.log.assignment.start", userId, userType, assignedBy));

        // Валидация дат
        validator.validateDateRange(validFrom, validTo);

        try {
            // Деактивируем предыдущие активные назначения
            deactivator.deactivatePrevious(userId);

            // Создаем новое назначение
            return creationProcessor.createAssignment(
                    userId, userType, assignedBy, reason, validFrom, validTo
            );

        } catch (Exception e) {
            log.error(messageService.get("user-type.log.assignment.error", userId, userType, e.getMessage()), e);
            throw new UserTypeAssignmentException(userId, userType.name(),
                    messageService.get("user-type.error.assignment.failed", e.getMessage()));
        }
    }

    @Transactional
    public UserTypeAssignmentDto assignUserType(Long userId, UserType userType, String assignedBy) {
        return assignUserType(userId, userType, assignedBy, null, null, null);
    }

    @Transactional
    public UserTypeAssignmentDto assignUserType(Long userId, UserType userType, String assignedBy, String reason) {
        return assignUserType(userId, userType, assignedBy, reason, null, null);
    }

    @Transactional
    public UserTypeAssignmentDto assignTemporaryUserType(Long userId, UserType userType, String assignedBy,
                                                         LocalDateTime validTo, String reason) {
        return assignUserType(userId, userType, assignedBy, reason, LocalDateTime.now(), validTo);
    }

    // ========== ПОЛУЧЕНИЕ ТЕКУЩЕГО ТИПА ==========

    @Transactional(readOnly = true)
    public UserTypeAssignmentDto getCurrentUserType(Long userId) {
        return queryProcessor.getCurrentUserType(userId);
    }

    @Transactional(readOnly = true)
    public UserTypeAssignmentDto getCurrentUserTypeOrThrow(Long userId) {
        return queryProcessor.getCurrentUserTypeOrThrow(userId);
    }

    // ========== ИСТОРИЯ ==========

    @Transactional(readOnly = true)
    public List<UserTypeAssignmentDto> getUserTypeHistory(Long userId) {
        return queryProcessor.getUserTypeHistory(userId);
    }

    // ========== ДЕАКТИВАЦИЯ ==========

    @Transactional
    public void deactivateCurrentUserType(Long userId) {
        deactivator.deactivateCurrent(userId);
    }

    // ========== РЕАКТИВАЦИЯ ==========

    @Transactional
    public UserTypeAssignmentDto reactivateUserType(Long assignmentId) {
        return reactivationProcessor.reactivate(assignmentId);
    }

    // ========== ПРОВЕРКИ И ПОИСК ==========

    @Transactional(readOnly = true)
    public boolean hasActiveUserType(Long userId, UserType userType) {
        return queryProcessor.hasActiveUserType(userId, userType);
    }

    @Transactional(readOnly = true)
    public List<Long> getUserIdsByUserType(UserType userType) {
        return queryProcessor.getUserIdsByUserType(userType);
    }
}