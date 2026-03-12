
package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.UserRepository;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRepository userRepository;
    private final UserSearchService userSearchService;
    private final MessageService messageService;

    @Transactional
    public User changeUserRole(User adminUser, Long userId, Role newRole, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        final Locale finalLocale = locale;
        final Long finalUserId = userId;
        final Role finalNewRole = newRole;
        final User finalAdminUser = adminUser;

        if (!finalAdminUser.getRole().canManage(finalNewRole)) {
            String errorMessage = messageService.get(
                    "user-role-service.error.role.insufficient.rights.assign",
                    finalNewRole
            );
            log.warn(messageService.get(
                    "user-role-service.log.role.insufficient.rights.assign",
                    finalAdminUser.getEmail(),
                    finalNewRole
            ));
            throw new AccessDeniedException(errorMessage);
        }

        User targetUser = userSearchService.getUserById(finalUserId);

        if (!finalAdminUser.getRole().canManage(targetUser.getRole())) {
            String errorMessage = messageService.get(
                    "user-role-service.error.role.insufficient.rights.manage",
                    targetUser.getRole()
            );
            log.warn(messageService.get(
                    "user-role-service.log.role.insufficient.rights.manage",
                    finalAdminUser.getEmail(),
                    targetUser.getRole()
            ));
            throw new AccessDeniedException(errorMessage);
        }

        User userToUpdate = userRepository.findById(finalUserId)
                .orElseThrow(() -> {
                    String errorMessage = messageService.get(
                            "user-role-service.error.user.not.found.id",
                            finalUserId
                    );
                    log.error(errorMessage);
                    return new RuntimeException(errorMessage);
                });

        if (userToUpdate.getId().equals(finalAdminUser.getId())) {
            String errorMessage = messageService.get(
                    "user-role-service.error.role.change.self"
            );
            log.warn(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        Role oldRole = userToUpdate.getRole();
        userToUpdate.setRole(finalNewRole);
        User updatedUser = userRepository.save(userToUpdate);

        log.info(messageService.get(
                "user-role-service.log.role.changed",
                finalAdminUser.getEmail(),
                updatedUser.getEmail(),
                oldRole,
                finalNewRole
        ));

        return updatedUser;
    }

    @Transactional(readOnly = true)
    public boolean canManageRole(User adminUser, Role targetRole, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        boolean canManage = adminUser.getRole().canManage(targetRole);

        log.debug(messageService.get(
                "user-role-service.log.role.can.manage",
                adminUser.getEmail(),
                targetRole,
                canManage
        ));

        return canManage;
    }

    @Transactional(readOnly = true)
    public boolean canChangeUserRole(User adminUser, Long userId, Role newRole, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        try {
            User targetUser = userSearchService.getUserById(userId);

            if (targetUser.getId().equals(adminUser.getId())) {
                log.debug(messageService.get(
                        "user-role-service.log.role.change.self.check",
                        adminUser.getEmail()
                ));
                return false;
            }

            if (!adminUser.getRole().canManage(newRole)) {
                log.debug(messageService.get(
                        "user-role-service.log.role.cannot.assign",
                        adminUser.getEmail(),
                        newRole
                ));
                return false;
            }

            if (!adminUser.getRole().canManage(targetUser.getRole())) {
                log.debug(messageService.get(
                        "user-role-service.log.role.cannot.manage.current",
                        adminUser.getEmail(),
                        targetUser.getRole()
                ));
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error(messageService.get(
                    "user-role-service.log.role.check.error",
                    userId,
                    e.getMessage()
            ));
            return false;
        }
    }

    @Transactional
    public User changeUserRole(User adminUser, Long userId, Role newRole) {
        return changeUserRole(adminUser, userId, newRole, Locale.getDefault());
    }

    @Transactional(readOnly = true)
    public boolean canManageRole(User adminUser, Role targetRole) {
        return canManageRole(adminUser, targetRole, Locale.getDefault());
    }
}