package ru.galtor85.household_store.processor.assignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.response.user.UserTypeAssignmentDto;
import ru.galtor85.household_store.entity.user.UserTypeAssignment;
import ru.galtor85.household_store.mapper.user.UserTypeAssignmentMapper;
import ru.galtor85.household_store.repository.user.UserTypeAssignmentRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.auth.UserTypeAssignmentValidator;

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