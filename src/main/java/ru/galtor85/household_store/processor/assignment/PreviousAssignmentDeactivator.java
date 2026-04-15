package ru.galtor85.household_store.processor.assignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.mapper.user.UserTypeAssignmentMapper;
import ru.galtor85.household_store.repository.user.UserTypeAssignmentRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

/**
 * Processor for deactivating previous user type assignments.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreviousAssignmentDeactivator {

    private final UserTypeAssignmentRepository assignmentRepository;
    private final UserTypeAssignmentMapper mapper;
    private final LogMessageService logMsg;

    /**
     * Deactivates the currently active assignment for a user.
     *
     * @param userId the user ID
     */
    @Transactional
    public void deactivatePrevious(Long userId) {
        assignmentRepository.findActiveByUserId(userId)
                .ifPresent(active -> {
                    assignmentRepository.save(mapper.deactivate(active));
                    log.debug(logMsg.get(
                            "user-type.log.previous.deactivated",
                            userId, active.getUserType()
                    ));
                });
    }
}