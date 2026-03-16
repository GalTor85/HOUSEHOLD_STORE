package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.InvalidDateRangeException;
import ru.galtor85.household_store.advice.exception.UserTypeAssignmentException;
import ru.galtor85.household_store.advice.exception.UserTypeAssignmentNotFoundException;
import ru.galtor85.household_store.dto.UserTypeAssignmentDto;
import ru.galtor85.household_store.entity.UserType;
import ru.galtor85.household_store.entity.UserTypeAssignment;
import ru.galtor85.household_store.repository.UserTypeAssignmentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserTypeAssignmentService {

    private final UserTypeAssignmentRepository assignmentRepository;
    private final MessageService messageService;

    /**
     * Назначение типа пользователя
     * @param userId ID пользователя
     * @param userType тип пользователя (RETAIL, WHOLESALE, VIP, CORPORATE, PARTNER, EMPLOYEE)
     * @param assignedBy кто назначил (email)
     * @param reason причина назначения
     * @param validFrom с какого времени действует (null = сейчас)
     * @param validTo до какого времени действует (null = бессрочно)
     * @return созданное назначение
     */
    @Transactional
    public UserTypeAssignment assignUserType(Long userId, UserType userType, String assignedBy,
                                             String reason, LocalDateTime validFrom, LocalDateTime validTo) {

        log.debug(messageService.get("user-type.log.assignment.start", userId, userType, assignedBy));

        // Валидация дат
        validateDateRange(validFrom, validTo);

        try {
            // Деактивируем предыдущие активные назначения
            assignmentRepository.findActiveByUserId(userId)
                    .ifPresent(active -> {
                        active.setActive(false);
                        assignmentRepository.save(active);
                        log.debug(messageService.get(
                                "user-type.log.previous.deactivated",
                                userId, active.getUserType()
                        ));
                    });

            UserTypeAssignment assignment = UserTypeAssignment.builder()
                    .userId(userId)
                    .userType(userType)
                    .assignedBy(assignedBy)
                    .reason(reason)
                    .validFrom(validFrom != null ? validFrom : LocalDateTime.now())
                    .validTo(validTo)
                    .active(true)
                    .build();

            UserTypeAssignment saved = assignmentRepository.save(assignment);

            log.info(messageService.get(
                    "user-type.log.assigned.success",
                    userType, userId, assignedBy
            ));

            return saved;

        } catch (Exception e) {
            log.error(messageService.get("user-type.log.assignment.error", userId, userType, e.getMessage()), e);
            throw new UserTypeAssignmentException(userId, userType.name(),
                    messageService.get("user-type.error.assignment.failed", e.getMessage()));
        }
    }

    /**
     * Назначение типа пользователя с автоматической деактивацией предыдущего
     */
    @Transactional
    public UserTypeAssignment assignUserType(Long userId, UserType userType, String assignedBy) {
        return assignUserType(userId, userType, assignedBy, null, null, null);
    }

    /**
     * Назначение типа пользователя с указанием причины
     */
    @Transactional
    public UserTypeAssignment assignUserType(Long userId, UserType userType, String assignedBy, String reason) {
        return assignUserType(userId, userType, assignedBy, reason, null, null);
    }

    /**
     * Назначение временного типа пользователя
     */
    @Transactional
    public UserTypeAssignment assignTemporaryUserType(Long userId, UserType userType, String assignedBy,
                                                      LocalDateTime validTo, String reason) {
        return assignUserType(userId, userType, assignedBy, reason, LocalDateTime.now(), validTo);
    }

    @Transactional(readOnly = true)
    public UserTypeAssignment getCurrentUserType(Long userId) {
        log.debug(messageService.get("user-type.log.get.current", userId));

        return assignmentRepository.findActiveByUserId(userId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public UserTypeAssignment getCurrentUserTypeOrThrow(Long userId) {
        log.debug(messageService.get("user-type.log.get.current", userId));

        return assignmentRepository.findActiveByUserId(userId)
                .orElseThrow(() -> {
                    log.warn(messageService.get("user-type.log.not.found", userId));
                    return new UserTypeAssignmentNotFoundException(userId);
                });
    }

    @Transactional(readOnly = true)
    public List<UserTypeAssignment> getUserTypeHistory(Long userId) {
        log.debug(messageService.get("user-type.log.get.history", userId));

        List<UserTypeAssignment> history = assignmentRepository.findByUserId(userId);

        log.debug(messageService.get("user-type.log.history.size", userId, history.size()));

        return history;
    }

    @Transactional
    public void deactivateCurrentUserType(Long userId) {
        log.debug(messageService.get("user-type.log.deactivate.start", userId));

        assignmentRepository.findActiveByUserId(userId)
                .ifPresentOrElse(
                        active -> {
                            active.setActive(false);
                            assignmentRepository.save(active);
                            log.info(messageService.get(
                                    "user-type.log.deactivated.success",
                                    userId, active.getUserType()
                            ));
                        },
                        () -> log.debug(messageService.get("user-type.log.no.active.assignment", userId))
                );
    }

    @Transactional
    public UserTypeAssignment reactivateUserType(Long assignmentId) {
        log.debug(messageService.get("user-type.log.reactivate.start", assignmentId));

        UserTypeAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-type.log.assignment.not.found", assignmentId));
                    return new UserTypeAssignmentException(null, null,
                            messageService.get("user-type.error.assignment.not.found", assignmentId));
                });

        if (assignment.isActive()) {
            log.debug(messageService.get("user-type.log.already.active", assignmentId));
            return assignment;
        }

        // Деактивируем текущее активное назначение
        assignmentRepository.findActiveByUserId(assignment.getUserId())
                .ifPresent(active -> {
                    active.setActive(false);
                    assignmentRepository.save(active);
                });

        assignment.setActive(true);
        assignment.setUpdatedAt(LocalDateTime.now());

        UserTypeAssignment reactivated = assignmentRepository.save(assignment);

        log.info(messageService.get(
                "user-type.log.reactivated.success",
                assignmentId, assignment.getUserId()
        ));

        return reactivated;
    }

    @Transactional(readOnly = true)
    public List<UserTypeAssignmentDto> getUserTypeAssignmentsWithLocalization(Long userId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        log.debug(messageService.get("user-type.log.get.history.localized", userId));

        List<UserTypeAssignment> assignments = assignmentRepository.findByUserId(userId);

        Locale finalLocale = locale;
        return assignments.stream()
                .map(assignment -> convertToDto(assignment, finalLocale))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean hasActiveUserType(Long userId, UserType userType) {
        return assignmentRepository.findActiveByUserId(userId)
                .map(assignment -> assignment.getUserType() == userType)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<Long> getUserIdsByUserType(UserType userType) {
        return assignmentRepository.findActiveByUserType(userType)
                .stream()
                .map(UserTypeAssignment::getUserId)
                .collect(Collectors.toList());
    }

    private void validateDateRange(LocalDateTime validFrom, LocalDateTime validTo) {
        if (validFrom != null && validTo != null && validFrom.isAfter(validTo)) {
            log.warn(messageService.get("user-type.log.invalid.date.range", validFrom, validTo));
            throw new InvalidDateRangeException(validFrom, validTo);
        }
    }

    private UserTypeAssignmentDto convertToDto(UserTypeAssignment assignment, Locale locale) {
        String localizedName = messageService.get(
                "usertype." + assignment.getUserType().name().toLowerCase()
        );

        return UserTypeAssignmentDto.builder()
                .id(assignment.getId())
                .userId(assignment.getUserId())
                .userType(assignment.getUserType())
                .userTypeName(localizedName)
                .assignedAt(assignment.getAssignedAt())
                .updatedAt(assignment.getUpdatedAt())
                .assignedBy(assignment.getAssignedBy())
                .active(assignment.isActive())
                .validFrom(assignment.getValidFrom())
                .validTo(assignment.getValidTo())
                .reason(assignment.getReason())
                .build();
    }
}