package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
                    String error = messageService.get("user-role-service.error.admin.security.not.found", adminUser.getEmail());
                    log.error(error);
                    return new AccessDeniedException(error);
                });

        // Проверяем, может ли админ назначать эту роль
        if (!adminSecurity.getRole().canManage(newRole)) {
            String errorMessage = messageService.get("user-role-service.error.role.insufficient.rights.assign", newRole);
            log.warn(messageService.get("user-role-service.log.role.insufficient.rights.assign", adminUser.getEmail(), newRole));
            throw new AccessDeniedException(errorMessage);
        }

        // Получаем целевого пользователя (бизнес-данные)
        User targetUser = userSearchService.getUserById(userId);

        // Получаем SecurityUser целевого пользователя
        SecurityUser targetSecurity = securityUserRepository.findById(userId)
                .orElseThrow(() -> {
                    String error = messageService.get("user-role-service.error.target.security.not.found", userId);
                    log.error(error);
                    return new RuntimeException(error);
                });

        // Проверяем, может ли админ управлять текущей ролью пользователя
        if (!adminSecurity.getRole().canManage(targetSecurity.getRole())) {
            String errorMessage = messageService.get("user-role-service.error.role.insufficient.rights.manage", targetSecurity.getRole());
            log.warn(messageService.get("user-role-service.log.role.insufficient.rights.manage", adminUser.getEmail(), targetSecurity.getRole()));
            throw new AccessDeniedException(errorMessage);
        }

        // Нельзя изменить свою собственную роль
        if (targetUser.getId().equals(adminUser.getId())) {
            String errorMessage = messageService.get("user-role-service.error.role.change.self");
            log.warn(errorMessage);
            throw new RuntimeException(errorMessage);
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
                    String error = messageService.get("user-role-service.error.admin.security.not.found", adminUser.getEmail());
                    log.error(error);
                    return new AccessDeniedException(error);
                });

        boolean canManage = adminSecurity.getRole().canManage(targetRole);

        log.debug(messageService.get("user-role-service.log.role.can.manage", adminUser.getEmail(), targetRole, canManage));

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
                    .orElseThrow(() -> new AccessDeniedException("Admin not found"));

            User targetUser = userSearchService.getUserById(userId);
            SecurityUser targetSecurity = securityUserRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Target security not found"));

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