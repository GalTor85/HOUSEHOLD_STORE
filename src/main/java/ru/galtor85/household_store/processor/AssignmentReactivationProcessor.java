package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.UserTypeAssignmentDto;
import ru.galtor85.household_store.entity.UserTypeAssignment;
import ru.galtor85.household_store.mapper.UserTypeAssignmentMapper;
import ru.galtor85.household_store.repository.UserTypeAssignmentRepository;
import ru.galtor85.household_store.service.MessageService;
import ru.galtor85.household_store.validator.UserTypeAssignmentValidator;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssignmentReactivationProcessor {

    private final UserTypeAssignmentRepository assignmentRepository;
    private final UserTypeAssignmentMapper mapper;
    private final UserTypeAssignmentValidator validator;
    private final MessageService messageService;
    private final PreviousAssignmentDeactivator deactivator;

    @Transactional
    public UserTypeAssignmentDto reactivate(Long assignmentId) {
        UserTypeAssignment assignment = validator.validateAssignmentExists(assignmentId);

        if (assignment.isActive()) {
            log.debug(messageService.get("user-type.log.already.active", assignmentId));
            return mapper.toDto(assignment);
        }

        // Деактивируем текущее активное назначение
        deactivator.deactivatePrevious(assignment.getUserId());

        UserTypeAssignment reactivated = assignmentRepository.save(mapper.reactivate(assignment));

        log.info(messageService.get(
                "user-type.log.reactivated.success",
                assignmentId, assignment.getUserId()
        ));

        return mapper.toDto(reactivated);
    }
}