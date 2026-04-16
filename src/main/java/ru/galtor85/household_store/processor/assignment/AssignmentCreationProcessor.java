package ru.galtor85.household_store.processor.assignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.entity.user.UserTypeAssignment;
import ru.galtor85.household_store.mapper.user.UserTypeAssignmentMapper;
import ru.galtor85.household_store.repository.user.UserTypeAssignmentRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.time.LocalDateTime;

/**
 * Processor for creating user type assignments.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssignmentCreationProcessor {

    private final UserTypeAssignmentRepository assignmentRepository;
    private final UserTypeAssignmentMapper mapper;
    private final LogMessageService logMsg;

    /**
     * Creates a new user type assignment.
     *
     * @param userId     the user ID
     * @param userType   the user type to assign
     * @param assignedBy identifier of who assigned the type
     * @param reason     reason for assignment
     * @param validFrom  validity start date (optional)
     * @param validTo    validity end date (optional)
     */
    @Transactional
    public void createAssignment(Long userId, UserType userType, String assignedBy,
                                 String reason, LocalDateTime validFrom, LocalDateTime validTo) {

        UserTypeAssignment assignment = mapper.createEntity(
                userId, userType, assignedBy, reason, validFrom, validTo
        );

        UserTypeAssignment saved = assignmentRepository.save(assignment);

        log.info(logMsg.get(
                "user-type.log.assigned.success",
                userType, userId, assignedBy
        ));

        mapper.toDto(saved);
    }
}