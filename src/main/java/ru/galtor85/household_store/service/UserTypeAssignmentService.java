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
import ru.galtor85.household_store.mapper.UserTypeAssignmentMapper;
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
    private final UserTypeAssignmentMapper mapper;
    private final MessageService messageService;

    /**
     * Назначение типа пользователя
     */
    @Transactional
    public UserTypeAssignmentDto assignUserType(Long userId, UserType userType, String assignedBy,
                                                String reason, LocalDateTime validFrom, LocalDateTime validTo,
                                                Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        log.debug(messageService.get("user-type.log.assignment.start", userId, userType, assignedBy));

        // Валидация дат
        validateDateRange(validFrom, validTo, locale);

        try {
            // Деактивируем предыдущие активные назначения
            assignmentRepository.findActiveByUserId(userId)
                    .ifPresent(active -> {
                        assignmentRepository.save(mapper.deactivate(active));
                        log.debug(messageService.get(
                                "user-type.log.previous.deactivated",
                                userId, active.getUserType()
                        ));
                    });

            // Создаем новое назначение через маппер
            UserTypeAssignment assignment = mapper.createEntity(
                    userId, userType, assignedBy, reason, validFrom, validTo
            );

            UserTypeAssignment saved = assignmentRepository.save(assignment);

            log.info(messageService.get(
                    "user-type.log.assigned.success",
                    userType, userId, assignedBy
            ));

            return mapper.toDto(saved, locale);

        } catch (Exception e) {
            log.error(messageService.get("user-type.log.assignment.error", userId, userType, e.getMessage()), e);
            throw new UserTypeAssignmentException(userId, userType.name(),
                    messageService.get("user-type.error.assignment.failed", e.getMessage()));
        }
    }

    /**
     * Назначение типа пользователя (без локали)
     */
    @Transactional
    public UserTypeAssignment assignUserType(Long userId, UserType userType, String assignedBy,
                                             String reason, LocalDateTime validFrom, LocalDateTime validTo) {
        return mapper.toEntity(assignUserType(userId, userType, assignedBy, reason, validFrom, validTo, Locale.getDefault()));
    }

    /**
     * Назначение типа пользователя с автоматической деактивацией предыдущего
     */
    @Transactional
    public UserTypeAssignmentDto assignUserType(Long userId, UserType userType, String assignedBy, Locale locale) {
        return assignUserType(userId, userType, assignedBy, null, null, null, locale);
    }

    @Transactional
    public UserTypeAssignment assignUserType(Long userId, UserType userType, String assignedBy) {
        return mapper.toEntity(assignUserType(userId, userType, assignedBy, Locale.getDefault()));
    }

    /**
     * Назначение типа пользователя с указанием причины
     */
    @Transactional
    public UserTypeAssignmentDto assignUserType(Long userId, UserType userType, String assignedBy,
                                                String reason, Locale locale) {
        return assignUserType(userId, userType, assignedBy, reason, null, null, locale);
    }

    @Transactional
    public UserTypeAssignment assignUserType(Long userId, UserType userType, String assignedBy, String reason) {
        return mapper.toEntity(assignUserType(userId, userType, assignedBy, reason, Locale.getDefault()));
    }

    /**
     * Назначение временного типа пользователя
     */
    @Transactional
    public UserTypeAssignmentDto assignTemporaryUserType(Long userId, UserType userType, String assignedBy,
                                                         LocalDateTime validTo, String reason, Locale locale) {
        return assignUserType(userId, userType, assignedBy, reason, LocalDateTime.now(), validTo, locale);
    }

    @Transactional
    public UserTypeAssignment assignTemporaryUserType(Long userId, UserType userType, String assignedBy,
                                                      LocalDateTime validTo, String reason) {
        return mapper.toEntity(assignTemporaryUserType(userId, userType, assignedBy, validTo, reason, Locale.getDefault()));
    }

    @Transactional(readOnly = true)
    public UserTypeAssignmentDto getCurrentUserType(Long userId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        log.debug(messageService.get("user-type.log.get.current", userId));

        Locale finalLocale = locale;
        return assignmentRepository.findActiveByUserId(userId)
                .map(assignment -> mapper.toDto(assignment, finalLocale))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public UserTypeAssignment getCurrentUserTypeEntity(Long userId) {
        log.debug(messageService.get("user-type.log.get.current", userId));
        return assignmentRepository.findActiveByUserId(userId).orElse(null);
    }

    @Transactional(readOnly = true)
    public UserTypeAssignmentDto getCurrentUserTypeOrThrow(Long userId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        log.debug(messageService.get("user-type.log.get.current", userId));

        Locale finalLocale = locale;
        return assignmentRepository.findActiveByUserId(userId)
                .map(assignment -> mapper.toDto(assignment, finalLocale))
                .orElseThrow(() -> {
                    log.warn(messageService.get("user-type.log.not.found", userId));
                    return new UserTypeAssignmentNotFoundException(userId);
                });
    }

    @Transactional(readOnly = true)
    public UserTypeAssignment getCurrentUserTypeEntityOrThrow(Long userId) {
        log.debug(messageService.get("user-type.log.get.current", userId));

        return assignmentRepository.findActiveByUserId(userId)
                .orElseThrow(() -> {
                    log.warn(messageService.get("user-type.log.not.found", userId));
                    return new UserTypeAssignmentNotFoundException(userId);
                });
    }

    @Transactional(readOnly = true)
    public List<UserTypeAssignmentDto> getUserTypeHistory(Long userId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        log.debug(messageService.get("user-type.log.get.history", userId));

        List<UserTypeAssignment> history = assignmentRepository.findByUserId(userId);

        log.debug(messageService.get("user-type.log.history.size", userId, history.size()));

        return mapper.toDtoList(history, locale);
    }

    @Transactional(readOnly = true)
    public List<UserTypeAssignment> getUserTypeHistoryEntity(Long userId) {
        log.debug(messageService.get("user-type.log.get.history", userId));
        return assignmentRepository.findByUserId(userId);
    }

    @Transactional
    public void deactivateCurrentUserType(Long userId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        log.debug(messageService.get("user-type.log.deactivate.start", userId));

        assignmentRepository.findActiveByUserId(userId)
                .ifPresentOrElse(
                        active -> {
                            assignmentRepository.save(mapper.deactivate(active));
                            log.info(messageService.get(
                                    "user-type.log.deactivated.success",
                                    userId, active.getUserType()
                            ));
                        },
                        () -> log.debug(messageService.get("user-type.log.no.active.assignment", userId))
                );
    }

    @Transactional
    public void deactivateCurrentUserType(Long userId) {
        deactivateCurrentUserType(userId, Locale.getDefault());
    }

    @Transactional
    public UserTypeAssignmentDto reactivateUserType(Long assignmentId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        log.debug(messageService.get("user-type.log.reactivate.start", assignmentId));

        UserTypeAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-type.log.assignment.not.found", assignmentId));
                    return new UserTypeAssignmentException(null, null,
                            messageService.get("user-type.error.assignment.not.found", assignmentId));
                });

        if (assignment.isActive()) {
            log.debug(messageService.get("user-type.log.already.active", assignmentId));
            return mapper.toDto(assignment, locale);
        }

        // Деактивируем текущее активное назначение
        assignmentRepository.findActiveByUserId(assignment.getUserId())
                .ifPresent(active -> assignmentRepository.save(mapper.deactivate(active)));

        UserTypeAssignment reactivated = assignmentRepository.save(mapper.reactivate(assignment));

        log.info(messageService.get(
                "user-type.log.reactivated.success",
                assignmentId, assignment.getUserId()
        ));

        return mapper.toDto(reactivated, locale);
    }

    @Transactional
    public UserTypeAssignment reactivateUserType(Long assignmentId) {
        return mapper.toEntity(reactivateUserType(assignmentId, Locale.getDefault()));
    }

    @Transactional(readOnly = true)
    public List<UserTypeAssignmentDto> getUserTypeAssignmentsWithLocalization(Long userId, Locale locale) {
        return getUserTypeHistory(userId, locale);
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

    private void validateDateRange(LocalDateTime validFrom, LocalDateTime validTo, Locale locale) {
        if (validFrom != null && validTo != null && validFrom.isAfter(validTo)) {
            log.warn(messageService.get("user-type.log.invalid.date.range", validFrom, validTo));
            throw new InvalidDateRangeException(validFrom, validTo);
        }
    }
}