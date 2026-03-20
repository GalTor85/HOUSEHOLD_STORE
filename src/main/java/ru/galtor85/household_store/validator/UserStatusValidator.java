package ru.galtor85.household_store.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.UserAccessException;
import ru.galtor85.household_store.advice.exception.UserNotFoundException;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.MessageService;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatusValidator {

    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;

    public SecurityUser validateAdminSecurityUser(Long adminId, String adminEmail) {
        return securityUserRepository.findById(adminId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-status-service.log.admin.security.not.found", adminEmail));
                    return new UserNotFoundException(adminId.toString());
                });
    }

    public SecurityUser validateTargetSecurityUser(Long userId) {
        return securityUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-status-service.log.user.not.found.id", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    public void validateAdminCanManageUser(SecurityUser adminSecurity, SecurityUser targetSecurity,
                                           User adminUser, User targetUser) {
        if (!adminSecurity.getRole().canManage(targetSecurity.getRole())) {
            log.warn(messageService.get(
                    "user-status-service.log.status.insufficient.rights.manage",
                    adminUser.getEmail(),
                    targetSecurity.getRole()
            ));
            throw new UserAccessException(
                    messageService.get("user-status-service.error.status.insufficient.rights.manage",
                            targetSecurity.getRole())
            );
        }
    }

    public void validateNotSelfDeactivate(User adminUser, User targetUser, boolean active) {
        if (targetUser.getId().equals(adminUser.getId()) && !active) {
            log.warn(messageService.get("user-status-service.log.status.deactivate.self", adminUser.getEmail()));
            throw new UserAccessException(
                    messageService.get("user-status-service.error.status.deactivate.self")
            );
        }
    }
}