package ru.galtor85.household_store.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.entity.user.UserTypeAssignment;
import ru.galtor85.household_store.processor.delete.HardDeleteProcessor;
import ru.galtor85.household_store.repository.user.UserTypeAssignmentRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.validator.auth.UserDeleteValidator;

import java.util.List;

/**
 * Service for deleting users.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletedService {

    private final UserDeleteValidator validator;
    private final HardDeleteProcessor hardDeleteProcessor;
    private final UserTypeAssignmentRepository userTypeAssignmentRepository;

    /**
     * Deletes a user after validating admin permissions.
     *
     * @param userId ID of user to delete
     * @param adminUser admin performing the deletion
     */
    @Transactional
    public void deleteUserWithCheck(Long userId, User adminUser) {
        User userToDelete = validator.validateUserExists(userId);
        SecurityUser targetSecurity = validator.validateSecurityUserExists(userId);
        SecurityUser adminSecurity = validator.validateAdminSecurityUserExists(
                adminUser.getId(), adminUser.getEmail());

        validator.validateNotSelfDelete(adminUser.getId(), userId);
        validator.validateAdminRights(adminSecurity, targetSecurity);

        List<UserTypeAssignment> assignments = userTypeAssignmentRepository.findByUserId(userId);
        if (!assignments.isEmpty()) {
            userTypeAssignmentRepository.deleteAll(assignments);
        }

        hardDeleteProcessor.deleteUserByAdmin(userToDelete, adminUser, userId);
    }
}