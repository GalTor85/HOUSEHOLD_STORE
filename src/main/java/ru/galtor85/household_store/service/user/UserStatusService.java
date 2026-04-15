package ru.galtor85.household_store.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.processor.user.UserStatusChangeProcessor;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.validator.auth.UserStatusValidator;

/**
 * Service for managing user account status.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatusService {

    private final UserSearchService userSearchService;
    private final LogMessageService logMsg;
    private final UserStatusValidator validator;
    private final UserStatusChangeProcessor changeProcessor;

    /**
     * Toggles user active status.
     *
     * @param adminUser admin performing the action
     * @param userId target user ID
     * @param active true to activate, false to deactivate
     * @return updated user entity
     */
    @Transactional
    public User toggleUserActive(User adminUser, Long userId, boolean active) {
        try {
            SecurityUser adminSecurity = validator.validateAdminSecurityUser(
                    adminUser.getId(), adminUser.getEmail());
            SecurityUser targetSecurity = validator.validateTargetSecurityUser(userId);
            User targetUser = userSearchService.getUserById(userId);

            validator.validateAdminCanManageUser(adminSecurity, targetSecurity, adminUser);
            validator.validateNotSelfDeactivate(adminUser, targetUser, active);

            changeProcessor.changeStatus(targetUser, targetSecurity, active, adminUser);

            return targetUser;
        } catch (Exception e) {
            log.error(logMsg.get("user-status-service.log.status.change.error",
                    userId, e.getMessage()), e);
            throw e;
        }
    }
}