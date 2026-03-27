package ru.galtor85.household_store.processor.assignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.response.user.UserTypeAssignmentDto;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.entity.user.UserTypeAssignment;
import ru.galtor85.household_store.mapper.user.UserTypeAssignmentMapper;
import ru.galtor85.household_store.repository.user.UserTypeAssignmentRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssignmentCreationProcessor {

    private final UserTypeAssignmentRepository assignmentRepository;
    private final UserTypeAssignmentMapper mapper;
    private final MessageService messageService;

    @Transactional
    public UserTypeAssignmentDto createAssignment(Long userId, UserType userType, String assignedBy,
                                                  String reason, LocalDateTime validFrom, LocalDateTime validTo) {

        UserTypeAssignment assignment = mapper.createEntity(
                userId, userType, assignedBy, reason, validFrom, validTo
        );

        UserTypeAssignment saved = assignmentRepository.save(assignment);

        log.info(messageService.get(
                "user-type.log.assigned.success",
                userType, userId, assignedBy
        ));

        return mapper.toDto(saved);
    }
}