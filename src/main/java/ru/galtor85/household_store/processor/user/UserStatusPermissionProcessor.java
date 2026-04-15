package ru.galtor85.household_store.processor.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.auth.UserNotFoundException;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.util.email.EmailMasker;

/**
 * Processor for checking user status permissions.
 *
 * <p>Provides methods to verify if an admin user has permission to manage
 * another user's account status, and to check the current active status
 * of any user.</p>
 *
 * <p>Permission rules:
 * <ul>
 *   <li>Admin cannot manage their own status</li>
 *   <li>Admin can only manage users with lower or equal role hierarchy</li>
 *   <li>Role hierarchy: ADMIN > MANAGER > USER</li>
 * </ul>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatusPermissionProcessor {

    private final SecurityUserRepository securityUserRepository;
    private final LogMessageService logMsg;
    private final EmailMasker emailMasker;

    /**
     * Checks if an admin user has permission to manage another user's status.
     *
     * <p>Performs the following checks:
     * <ol>
     *   <li>Ensures admin is not trying to manage their own status</li>
     *   <li>Verifies role hierarchy allows the operation</li>
     * </ol>
     *
     * @param adminUser the admin user attempting to manage status
     * @param targetUserId the ID of the user whose status would be changed
     * @return true if admin has permission, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean canManageUserStatus(User adminUser, Long targetUserId) {
        String maskedAdminEmail = emailMasker.maskEmail(adminUser.getEmail());

        try {
            SecurityUser adminSecurity = securityUserRepository.findById(adminUser.getId())
                    .orElseThrow(() -> new UserNotFoundException(adminUser.getId().toString()));

            SecurityUser targetSecurity = securityUserRepository.findById(targetUserId)
                    .orElseThrow(() -> new UserNotFoundException(targetUserId.toString()));

            if (targetUserId.equals(adminUser.getId())) {
                log.debug(logMsg.get("user-status-service.log.status.cannot.manage.self",
                        maskedAdminEmail));
                return false;
            }

            boolean canManage = adminSecurity.getRole().canManage(targetSecurity.getRole());

            log.debug(logMsg.get(
                    "user-status-service.log.status.can.manage",
                    maskedAdminEmail,
                    targetUserId,
                    canManage
            ));

            return canManage;

        } catch (UserNotFoundException e) {
            log.warn(logMsg.get("user.status.permission.user.not.found",
                    targetUserId, maskedAdminEmail));
            return false;
        } catch (Exception e) {
            log.error(logMsg.get("user-status-service.log.status.check.error",
                    targetUserId, e.getMessage()), e);
            return false;
        }
    }

    /**
     * Checks if a user account is currently active.
     *
     * <p>Returns false if the user is not found or if any error occurs
     * during the check.</p>
     *
     * @param userId the ID of the user to check
     * @return true if user exists and is active, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isUserActive(Long userId) {
        return securityUserRepository.findById(userId)
                .map(SecurityUser::isEnabled)
                .orElse(false);
    }
}