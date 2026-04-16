package ru.galtor85.household_store.validator.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.auth.UserAccessException;
import ru.galtor85.household_store.advice.exception.auth.UserNotFoundException;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.email.EmailMasker;

/**
 * Validator for user status operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatusValidator {

    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;
    private final EmailMasker emailMasker;

    /**
     * Validates admin security user exists.
     *
     * @param adminId admin user ID
     * @param adminEmail admin email for logging
     * @return admin security user
     * @throws UserNotFoundException if not found
     */
    public SecurityUser validateAdminSecurityUser(Long adminId, String adminEmail) {
        return securityUserRepository.findById(adminId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("user-status-service.log.admin.security.not.found",
                            emailMasker.maskEmail(adminEmail)));
                    return new UserNotFoundException(adminId.toString());
                });
    }

    /**
     * Validates target security user exists.
     *
     * @param userId target user ID
     * @return target security user
     * @throws UserNotFoundException if not found
     */
    public SecurityUser validateTargetSecurityUser(Long userId) {
        return securityUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("user-status-service.log.user.not.found.id", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    /**
     * Validates admin can manage target user.
     *
     * @param adminSecurity admin security user
     * @param targetSecurity target security user
     * @param adminUser admin user for logging
     * @throws UserAccessException if insufficient rights
     */
    public void validateAdminCanManageUser(SecurityUser adminSecurity, SecurityUser targetSecurity,
                                           User adminUser) {
        if (adminSecurity.getRole().cannotManage(targetSecurity.getRole())) {
            log.warn(logMsg.get(
                    "user-status-service.log.status.insufficient.rights.manage",
                    emailMasker.maskEmail(adminUser.getEmail()),
                    targetSecurity.getRole()
            ));
            throw new UserAccessException(
                    messageService.get("user-status-service.error.status.insufficient.rights.manage",
                            targetSecurity.getRole())
            );
        }
    }

    /**
     * Validates admin is not deactivating themselves.
     *
     * @param adminUser admin user
     * @param targetUser target user
     * @param active new status (true=activate, false=deactivate)
     * @throws UserAccessException if self deactivation attempted
     */
    public void validateNotSelfDeactivate(User adminUser, User targetUser, boolean active) {
        if (targetUser.getId().equals(adminUser.getId()) && !active) {
            log.warn(logMsg.get("user-status-service.log.status.deactivate.self",
                    emailMasker.maskEmail(adminUser.getEmail())));
            throw new UserAccessException(
                    messageService.get("user-status-service.error.status.deactivate.self")
            );
        }
    }
}