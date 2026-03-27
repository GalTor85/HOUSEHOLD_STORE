package ru.galtor85.household_store.processor.role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.user.UserSearchService;

@Slf4j
@Component
@RequiredArgsConstructor
public class RolePermissionProcessor {

    private final SecurityUserRepository securityUserRepository;
    private final UserSearchService userSearchService;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public boolean canManageRole(User adminUser, Role targetRole) {
        SecurityUser adminSecurity = securityUserRepository.findById(adminUser.getId())
                .orElseThrow(() -> new RuntimeException(messageService.get("role.permission.processor.role.admin.not.found")));

        boolean canManage = adminSecurity.getRole().canManage(targetRole);

        log.debug(messageService.get(
                "user-role-service.log.role.can.manage",
                adminUser.getEmail(),
                targetRole,
                canManage
        ));

        return canManage;
    }

    @Transactional(readOnly = true)
    public boolean canChangeUserRole(User adminUser, Long userId, Role newRole) {
        try {
            SecurityUser adminSecurity = securityUserRepository.findById(adminUser.getId())
                    .orElseThrow(() -> new RuntimeException(messageService.get("role.permission.processor.role.admin.not.found")));

            User targetUser = userSearchService.getUserById(userId);
            SecurityUser targetSecurity = securityUserRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException(messageService.get("role.permission.processor.target.user.not.found")));

            if (targetUser.getId().equals(adminUser.getId())) {
                log.debug(messageService.get("user-role-service.log.role.change.self.check", adminUser.getEmail()));
                return false;
            }

            if (!adminSecurity.getRole().canManage(newRole)) {
                log.debug(messageService.get("user-role-service.log.role.cannot.assign",
                        adminUser.getEmail(), newRole));
                return false;
            }

            if (!adminSecurity.getRole().canManage(targetSecurity.getRole())) {
                log.debug(messageService.get("user-role-service.log.role.cannot.manage.current",
                        adminUser.getEmail(), targetSecurity.getRole()));
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error(messageService.get("user-role-service.log.role.check.error", userId, e.getMessage()), e);
            return false;
        }
    }
}