package ru.galtor85.household_store.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.UserAccessException;
import ru.galtor85.household_store.advice.exception.UserNotFoundException;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.repository.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.MessageService;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleValidator {

    private final SecurityUserRepository securityUserRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    public SecurityUser validateAdminSecurityUser(Long adminId, String adminEmail) {
        return securityUserRepository.findById(adminId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-role-service.log.admin.security.not.found", adminEmail));
                    return new UserNotFoundException(adminId.toString());
                });
    }

    public SecurityUser validateTargetSecurityUser(Long userId) {
        return securityUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-role-service.log.target.security.not.found", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    public User validateTargetUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-role-service.log.target.user.not.found", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    public void validateAdminCanAssignRole(SecurityUser adminSecurity, Role newRole) {
        if (!adminSecurity.getRole().canManage(newRole)) {
            log.warn(messageService.get(
                    "user-role-service.log.role.insufficient.rights.assign",
                    adminSecurity.getUsername(),
                    newRole
            ));
            throw new UserAccessException(
                    messageService.get("user-role-service.error.role.insufficient.rights.assign", newRole)
            );
        }
    }

    public void validateAdminCanManageCurrentRole(SecurityUser adminSecurity, SecurityUser targetSecurity) {
        if (!adminSecurity.getRole().canManage(targetSecurity.getRole())) {
            log.warn(messageService.get(
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

    public void validateNotSelfRoleChange(Long adminId, Long targetUserId) {
        if (adminId.equals(targetUserId)) {
            log.warn(messageService.get("user-role-service.log.role.change.self", adminId));
            throw new UserAccessException(
                    messageService.get("user-role-service.error.role.change.self")
            );
        }
    }
}