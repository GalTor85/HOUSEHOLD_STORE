package ru.galtor85.household_store.processor.role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.auth.UserNotFoundException;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.user.UserSearchService;

/**
 * Processor for role permission checks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RolePermissionProcessor {

    private final SecurityUserRepository securityUserRepository;
    private final UserSearchService userSearchService;
    private final MessageService messageService;
    private final LogMessageService logMsg;

    /**
     * Checks if an admin can manage a target role.
     *
     * @param adminUser  the admin user
     * @param targetRole the target role
     * @return true if admin can manage the role
     */
    @Transactional(readOnly = true)
    public boolean canManageRole(User adminUser, Role targetRole) {
        SecurityUser adminSecurity = securityUserRepository.findById(adminUser.getId())
                .orElseThrow(() -> new RuntimeException(
                        messageService.get("role.permission.processor.role.admin.not.found")));

        boolean canManage = adminSecurity.getRole().canManage(targetRole);

        log.debug(logMsg.get(
                "user-role-service.log.role.can.manage",
                adminUser.getEmail(),
                targetRole,
                canManage
        ));

        return canManage;
    }

    /**
     * Checks if an admin can change a user's role.
     *
     * @param adminUser the admin user
     * @param userId    the target user ID
     * @param newRole   the new role
     * @return true if role change is allowed
     */
    @Transactional(readOnly = true)
    public boolean canChangeUserRole(User adminUser, Long userId, Role newRole) {
        try {
            SecurityUser adminSecurity = securityUserRepository.findById(adminUser.getId())
                    .orElseThrow(() -> new UserNotFoundException(adminUser.getId().toString()));

            User targetUser = userSearchService.getUserById(userId);
            SecurityUser targetSecurity = securityUserRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId.toString()));

            if (targetUser.getId().equals(adminUser.getId())) {
                log.debug(logMsg.get("user-role-service.log.role.change.self.check", adminUser.getEmail()));
                return false;
            }

            if (!adminSecurity.getRole().canManage(newRole)) {
                log.debug(logMsg.get("user-role-service.log.role.cannot.assign",
                        adminUser.getEmail(), newRole));
                return false;
            }

            if (!adminSecurity.getRole().canManage(targetSecurity.getRole())) {
                log.debug(logMsg.get("user-role-service.log.role.cannot.manage.current",
                        adminUser.getEmail(), targetSecurity.getRole()));
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error(logMsg.get("user-role-service.log.role.check.error", userId, e.getMessage()), e);
            return false;
        }
    }
}