package ru.galtor85.household_store.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.auth.UserAccessException;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.processor.role.RoleChangeProcessor;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.util.logger.RoleChangeLogger;
import ru.galtor85.household_store.validator.auth.RoleValidator;

/**
 * Service for managing user roles.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final RoleValidator validator;
    private final RoleChangeProcessor changeProcessor;
    private final RoleChangeLogger changeLogger;

    /**
     * Changes role for a user.
     *
     * @param adminUser admin performing the change
     * @param userId target user ID
     * @param newRole new role to assign
     * @return updated user entity
     * @throws UserAccessException if admin lacks permission
     */
    @Transactional
    public User changeUserRole(User adminUser, Long userId, Role newRole) {
        try {
            SecurityUser adminSecurity = validator.validateAdminSecurityUser(adminUser.getId(), adminUser.getEmail());
            User targetUser = validator.validateTargetUser(userId);
            SecurityUser targetSecurity = validator.validateTargetSecurityUser(userId);

            validator.validateAdminCanAssignRole(adminSecurity, newRole);
            validator.validateAdminCanManageCurrentRole(adminSecurity, targetSecurity);
            validator.validateNotSelfRoleChange(adminUser.getId(), userId);

            Role oldRole = targetSecurity.getRole();
            changeProcessor.changeRole(targetUser, targetSecurity, newRole, adminUser);
            changeLogger.logRoleChange(adminUser, targetUser, oldRole, newRole);

            return targetUser;

        } catch (UserAccessException e) {
            changeLogger.logFailedAttempt(adminUser, userId, newRole, e.getMessage());
            throw e;
        } catch (Exception e) {
            changeLogger.logFailedAttempt(adminUser, userId, newRole, "Unexpected error: " + e.getMessage());
            throw e;
        }
    }
}