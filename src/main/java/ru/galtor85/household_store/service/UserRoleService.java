package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.repository.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.security.SecurityUserFactory;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRepository userRepository;
    private final SecurityUserRepository securityUserRepository;
    private final UserSearchService userSearchService;
    private final MessageService messageService;
    private final SecurityUserFactory securityUserFactory;

    @Transactional
    public User changeUserRole(User adminUser, Long userId, Role newRole, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        // Получаем SecurityUser администратора
        SecurityUser adminSecurity = securityUserRepository.findById(adminUser.getId())
                .orElseThrow(() -> {
                    log.error(messageService.get("user-role-service.log.admin.security.not.found", adminUser.getEmail()));
                    return new UserNotFoundException(adminUser.getId().toString());
                });

        // Проверяем, может ли админ назначать эту роль
        if (!adminSecurity.getRole().canManage(newRole)) {
            log.warn(messageService.get(
                    "user-role-service.log.role.insufficient.rights.assign",
                    adminUser.getEmail(),
                    newRole
            ));
            throw new UserAccessException(
                    messageService.get("user-role-service.error.role.insufficient.rights.assign", newRole)
            );
        }

        // Получаем целевого пользователя (бизнес-данные)
        User targetUser = userSearchService.getUserById(userId, locale);

        // Получаем SecurityUser целевого пользователя
        SecurityUser targetSecurity = securityUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-role-service.log.target.security.not.found", userId));
                    return new UserNotFoundException(userId.toString());
                });

        // Проверяем, может ли админ управлять текущей ролью пользователя
        if (!adminSecurity.getRole().canManage(targetSecurity.getRole())) {
            log.warn(messageService.get(
                    "user-role-service.log.role.insufficient.rights.manage",
                    adminUser.getEmail(),
                    targetSecurity.getRole()
            ));
            throw new UserAccessException(
                    messageService.get("user-role-service.error.role.insufficient.rights.manage", targetSecurity.getRole())
            );
        }

        // Нельзя изменить свою собственную роль
        if (targetUser.getId().equals(adminUser.getId())) {
            log.warn(messageService.get("user-role-service.log.role.change.self", adminUser.getEmail()));
            throw new UserAccessException(
                    messageService.get("user-role-service.error.role.change.self")
            );
        }

        // Сохраняем старую роль для логирования
        Role oldRole = targetSecurity.getRole();

        // ИСПРАВЛЕНО: передаем существующий SecurityUser в фабрику
        SecurityUser updatedSecurityUser = securityUserFactory.withUpdatedRole(
                targetUser,
                targetSecurity,  // передаем существующий SecurityUser
                newRole
        );

        securityUserRepository.save(updatedSecurityUser);

        log.info(messageService.get(
                "user-role-service.log.role.changed",
                adminUser.getEmail(),
                targetUser.getEmail(),
                oldRole,
                newRole
        ));

        return targetUser;
    }

    @Transactional
    public User changeUserRole(User adminUser, Long userId, Role newRole) {
        return changeUserRole(adminUser, userId, newRole, Locale.getDefault());
    }

    @Transactional(readOnly = true)
    public boolean canManageRole(User adminUser, Role targetRole, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        SecurityUser adminSecurity = securityUserRepository.findById(adminUser.getId())
                .orElseThrow(() -> {
                    log.error(messageService.get("user-role-service.log.admin.security.not.found", adminUser.getEmail()));
                    return new UserNotFoundException(adminUser.getId().toString());
                });

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
    public boolean canManageRole(User adminUser, Role targetRole) {
        return canManageRole(adminUser, targetRole, Locale.getDefault());
    }

    @Transactional(readOnly = true)
    public boolean canChangeUserRole(User adminUser, Long userId, Role newRole, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        try {
            SecurityUser adminSecurity = securityUserRepository.findById(adminUser.getId())
                    .orElseThrow(() -> new UserNotFoundException(adminUser.getId().toString()));

            User targetUser = userSearchService.getUserById(userId, locale);
            SecurityUser targetSecurity = securityUserRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId.toString()));

            if (targetUser.getId().equals(adminUser.getId())) {
                log.debug(messageService.get("user-role-service.log.role.change.self.check", adminUser.getEmail()));
                return false;
            }

            if (!adminSecurity.getRole().canManage(newRole)) {
                log.debug(messageService.get("user-role-service.log.role.cannot.assign", adminUser.getEmail(), newRole));
                return false;
            }

            if (!adminSecurity.getRole().canManage(targetSecurity.getRole())) {
                log.debug(messageService.get("user-role-service.log.role.cannot.manage.current", adminUser.getEmail(), targetSecurity.getRole()));
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error(messageService.get("user-role-service.log.role.check.error", userId, e.getMessage()));
            return false;
        }
    }
}