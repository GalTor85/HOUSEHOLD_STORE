package ru.galtor85.household_store.processor.assignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.response.user.UserTypeAssignmentDto;
import ru.galtor85.household_store.mapper.user.UserTypeAssignmentMapper;
import ru.galtor85.household_store.repository.user.UserTypeAssignmentRepository;


/**
 * Processor for querying user type assignments.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssignmentQueryProcessor {

    private final UserTypeAssignmentRepository assignmentRepository;
    private final UserTypeAssignmentMapper mapper;

    /**
     * Gets the current active user type for a user.
     *
     * @param userId the user ID
     * @return UserTypeAssignmentDto or null if none
     */
    @Transactional(readOnly = true)
    public UserTypeAssignmentDto getCurrentUserType(Long userId) {
        return assignmentRepository.findActiveByUserId(userId)
                .map(mapper::toDto)
                .orElse(null);
    }
}