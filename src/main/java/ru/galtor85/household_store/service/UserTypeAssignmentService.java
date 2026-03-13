package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

        // Деактивируем предыдущие активные назначения
        assignmentRepository.findActiveByUserId(userId)
                .ifPresent(active -> {
                    active.setActive(false);
                    assignmentRepository.save(active);
                    log.debug(messageService.get(
                            "user-type-assignment.service.previous.deactivated",
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
                "user-type-assignment.service.assigned",
                userType, userId, assignedBy
        ));

        return saved;
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
        return assignmentRepository.findActiveByUserId(userId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<UserTypeAssignment> getUserTypeHistory(Long userId) {
        return assignmentRepository.findByUserId(userId);
    }

    @Transactional
    public void deactivateCurrentUserType(Long userId) {
        assignmentRepository.findActiveByUserId(userId)
                .ifPresent(active -> {
                    active.setActive(false);
                    assignmentRepository.save(active);
                    log.info(messageService.get(
                            "user-type-assignment.service.deactivated",
                            userId
                    ));
                });
    }

    @Transactional(readOnly = true)
    public List<UserTypeAssignmentDto> getUserTypeAssignmentsWithLocalization(Long userId, Locale locale) {
        List<UserTypeAssignment> assignments = assignmentRepository.findByUserId(userId);

        return assignments.stream()
                .map(assignment -> convertToDto(assignment, locale))
                .collect(Collectors.toList());
    }

    private UserTypeAssignmentDto convertToDto(UserTypeAssignment assignment, Locale locale) {
        String localizedName = messageService.get(
                "usertype." + assignment.getUserType().name()
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