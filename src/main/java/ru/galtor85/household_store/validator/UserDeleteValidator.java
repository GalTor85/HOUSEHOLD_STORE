package ru.galtor85.household_store.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.UserAccessException;
import ru.galtor85.household_store.advice.exception.UserNotFoundException;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.repository.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeleteValidator {

    private final UserRepository userRepository;
    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;

    public User validateUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-deleted-service.log.user.not.found.id", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    public SecurityUser validateSecurityUserExists(Long userId) {
        return securityUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-deleted-service.log.target.security.not.found", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    public SecurityUser validateAdminSecurityUserExists(Long adminId, String adminEmail) {
        return securityUserRepository.findById(adminId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-deleted-service.log.admin.security.not.found", adminEmail));
                    return new UserNotFoundException(adminId.toString());
                });
    }

    public void validateNotSelfDelete(Long adminId, Long userId) {
        if (adminId.equals(userId)) {
            log.warn(messageService.get("user-deleted-service.log.user.delete.self"));
            throw new UserAccessException(
                    messageService.get("user-deleted-service.error.user.delete.self")
            );
        }
    }

    public void validateAdminRights(SecurityUser adminSecurity, SecurityUser targetSecurity) {
        if (!adminSecurity.getRole().canManage(targetSecurity.getRole())) {
            log.warn(messageService.get(
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