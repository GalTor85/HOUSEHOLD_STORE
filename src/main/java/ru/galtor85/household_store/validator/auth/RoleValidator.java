package ru.galtor85.household_store.validator.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.auth.UserAccessException;
import ru.galtor85.household_store.advice.exception.auth.UserNotFoundException;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Validator for role management operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleValidator {

    private final SecurityUserRepository securityUserRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;

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
                    log.error(logMsg.get("user-role-service.log.admin.security.not.found", adminEmail));
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
                    log.error(logMsg.get("user-role-service.log.target.security.not.found", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    /**
     * Validates target user exists.
     *
     * @param userId target user ID
     * @return target user
     * @throws UserNotFoundException if not found
     */
    public User validateTargetUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("user-role-service.log.target.user.not.found", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    /**
     * Validates admin can assign the role.
     *
     * @param adminSecurity admin security user
     * @param newRole role to assign
     * @throws UserAccessException if insufficient rights
     */
    public void validateAdminCanAssignRole(SecurityUser adminSecurity, Role newRole) {
        if (!adminSecurity.getRole().canManage(newRole)) {
            log.warn(logMsg.get(
                    "user-role-service.log.role.insufficient.rights.assign",
                    adminSecurity.getUsername(),
                    newRole
            ));
            throw new UserAccessException(
                    messageService.get("user-role-service.error.role.insufficient.rights.assign", newRole)
            );
        }
    }

    /**
     * Validates admin can manage target user's current role.
     *
     * @param adminSecurity admin security user
     * @param targetSecurity target security user
     * @throws UserAccessException if insufficient rights
     */
    public void validateAdminCanManageCurrentRole(SecurityUser adminSecurity, SecurityUser targetSecurity) {
        if (!adminSecurity.getRole().canManage(targetSecurity.getRole())) {
            log.warn(logMsg.get(
                    "user-role-service.log.role.insufficient.rights.manage",
                    adminSecurity.getUsername(),
                    targetSecurity.getRole()
            ));
            throw new UserAccessException(
                    messageService.get("user-role-service.error.role.insufficient.rights.manage",
                            targetSecurity.getRole())
            );
        }
    }

    /**
     * Validates admin is not changing their own role.
     *
     * @param adminId admin user ID
     * @param targetUserId target user ID
     * @throws UserAccessException if self role change attempted
     */
    public void validateNotSelfRoleChange(Long adminId, Long targetUserId) {
        if (adminId.equals(targetUserId)) {
            log.warn(logMsg.get("user-role-service.log.role.change.self", adminId));
            throw new UserAccessException(
                    messageService.get("user-role-service.error.role.change.self")
            );
        }
    }
}