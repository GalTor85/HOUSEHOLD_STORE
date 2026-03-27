package ru.galtor85.household_store.processor.assignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.mapper.user.UserTypeAssignmentMapper;
import ru.galtor85.household_store.repository.user.UserTypeAssignmentRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class PreviousAssignmentDeactivator {

    private final UserTypeAssignmentRepository assignmentRepository;
    private final UserTypeAssignmentMapper mapper;
    private final MessageService messageService;

    @Transactional
    public void deactivatePrevious(Long userId) {
        assignmentRepository.findActiveByUserId(userId)
                .ifPresent(active -> {
                    assignmentRepository.save(mapper.deactivate(active));
                    log.debug(messageService.get(
                            "user-type.log.previous.deactivated",
                            userId, active.getUserType()
                    ));
                });
    }

    @Transactional
    public void deactivateCurrent(Long userId) {
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
}