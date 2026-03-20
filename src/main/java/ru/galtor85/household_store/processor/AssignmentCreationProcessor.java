package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.UserTypeAssignmentDto;
import ru.galtor85.household_store.entity.UserType;
import ru.galtor85.household_store.entity.UserTypeAssignment;
import ru.galtor85.household_store.mapper.UserTypeAssignmentMapper;
import ru.galtor85.household_store.repository.UserTypeAssignmentRepository;
import ru.galtor85.household_store.service.MessageService;

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