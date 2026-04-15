package ru.galtor85.household_store.validator.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.auth.UserAccessException;
import ru.galtor85.household_store.advice.exception.auth.UserNotFoundException;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Validator for user deletion operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeleteValidator {

    private final UserRepository userRepository;
    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;

    /**
     * Validates user exists.
     *
     * @param userId user ID
     * @return user entity
     * @throws UserNotFoundException if not found
     */
    public User validateUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("user-deleted-service.log.user.not.found.id", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    /**
     * Validates security user exists.
     *
     * @param userId user ID
     * @return security user entity
     * @throws UserNotFoundException if not found
     */
    public SecurityUser validateSecurityUserExists(Long userId) {
        return securityUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("user-deleted-service.log.target.security.not.found", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    /**
     * Validates admin security user exists.
     *
     * @param adminId admin user ID
     * @param adminEmail admin email for logging
     * @return admin security user
     * @throws UserNotFoundException if not found
     */
    public SecurityUser validateAdminSecurityUserExists(Long adminId, String adminEmail) {
        return securityUserRepository.findById(adminId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("user-deleted-service.log.admin.security.not.found", adminEmail));
                    return new UserNotFoundException(adminId.toString());
                });
    }

    /**
     * Validates admin is not deleting themselves.
     *
     * @param adminId admin user ID
     * @param userId target user ID
     * @throws UserAccessException if self delete attempted
     */
    public void validateNotSelfDelete(Long adminId, Long userId) {
        if (adminId.equals(userId)) {
            log.warn(logMsg.get("user-deleted-service.log.user.delete.self"));
            throw new UserAccessException(
                    messageService.get("user-deleted-service.error.user.delete.self")
            );
        }
    }

    /**
     * Validates admin has rights to delete target user.
     *
     * @param adminSecurity admin security user
     * @param targetSecurity target security user
     * @throws UserAccessException if insufficient rights
     */
    public void validateAdminRights(SecurityUser adminSecurity, SecurityUser targetSecurity) {
        if (!adminSecurity.getRole().canManage(targetSecurity.getRole())) {
            log.warn(logMsg.get(
                    "user-deleted-service.log.user.delete.insufficient.rights",
                    adminSecurity.getUsername(),
                    targetSecurity.getRole()
            ));
            throw new UserAccessException(
                    messageService.get("user-deleted-service.error.user.delete.insufficient.rights",
                            targetSecurity.getRole())
            );
        }
    }
}