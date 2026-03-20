package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.UserAccessException;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.processor.RoleChangeProcessor;
import ru.galtor85.household_store.processor.RolePermissionProcessor;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.util.RoleChangeLogger;
import ru.galtor85.household_store.validator.RoleValidator;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleService {

    // Валидаторы
    private final RoleValidator validator;

    // Процессоры
    private final RolePermissionProcessor permissionProcessor;
    private final RoleChangeProcessor changeProcessor;

    // Утилиты
    private final RoleChangeLogger changeLogger;

    // ========== ИЗМЕНЕНИЕ РОЛИ ==========

    @Transactional
    public User changeUserRole(User adminUser, Long userId, Role newRole) {
        try {
            // 1. Валидация администратора
            SecurityUser adminSecurity = validator.validateAdminSecurityUser(adminUser.getId(), adminUser.getEmail());

            // 2. Валидация целевого пользователя
            User targetUser = validator.validateTargetUser(userId);
            SecurityUser targetSecurity = validator.validateTargetSecurityUser(userId);

            // 3. Проверка прав
            validator.validateAdminCanAssignRole(adminSecurity, newRole);
            validator.validateAdminCanManageCurrentRole(adminSecurity, targetSecurity);
            validator.validateNotSelfRoleChange(adminUser.getId(), userId);

            // 4. Сохраняем старую роль для логирования
            Role oldRole = targetSecurity.getRole();

            // 5. Изменяем роль
            changeProcessor.changeRole(targetUser, targetSecurity, newRole, adminUser);

            // 6. Логируем успешное изменение
            changeLogger.logRoleChange(adminUser, targetUser, oldRole, newRole);

            return targetUser;

        } catch (UserAccessException e) {
            changeLogger.logFailedAttempt(adminUser, userId, newRole, e.getMessage());
            throw e;
        } catch (Exception e) {
            changeLogger.logFailedAttempt(adminUser, userId, newRole, "Unexpected error: " + e.getMessage());
            throw e;
        }
    }

    // ========== ПРОВЕРКА ПРАВ ==========

    @Transactional(readOnly = true)
    public boolean canManageRole(User adminUser, Role targetRole) {
        return permissionProcessor.canManageRole(adminUser, targetRole);
    }

    @Transactional(readOnly = true)
    public boolean canChangeUserRole(User adminUser, Long userId, Role newRole) {
        return permissionProcessor.canChangeUserRole(adminUser, userId, newRole);
    }
}