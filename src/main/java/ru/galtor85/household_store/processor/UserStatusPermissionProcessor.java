package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.UserNotFoundException;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatusPermissionProcessor {

    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public boolean canManageUserStatus(User adminUser, Long userId) {
        try {
            SecurityUser adminSecurity = securityUserRepository.findById(adminUser.getId())
                    .orElseThrow(() -> new UserNotFoundException(adminUser.getId().toString()));

            SecurityUser targetSecurity = securityUserRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId.toString()));

            if (userId.equals(adminUser.getId())) {
                log.debug(messageService.get("user-status-service.log.status.cannot.manage.self",
                        adminUser.getEmail()));
                return false;
            }

            boolean canManage = adminSecurity.getRole().canManage(targetSecurity.getRole());

            log.debug(messageService.get(
                    "user-status-service.log.status.can.manage",
                    adminUser.getEmail(),
                    userId,
                    canManage
            ));

            return canManage;

        } catch (Exception e) {
            log.error(messageService.get("user-status-service.log.status.check.error",
                    userId, e.getMessage()), e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean isUserActive(Long userId) {
        return securityUserRepository.findById(userId)
                .map(securityUser -> {
                    log.debug(messageService.get("user-status-service.log.status.check",
                            userId, securityUser.isEnabled()));
                    return securityUser.isEnabled();
                })
                .orElseGet(() -> {
                    log.debug(messageService.get("user-status-service.log.status.user.not.found",
                            userId));
                    return false;
                });
    }
}