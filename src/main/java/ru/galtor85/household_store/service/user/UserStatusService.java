package ru.galtor85.household_store.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.processor.user.UserStatusChangeProcessor;
import ru.galtor85.household_store.processor.user.UserStatusPermissionProcessor;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.auth.UserStatusValidator;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatusService {

    private final UserSearchService userSearchService;
    private final MessageService messageService;

    // Валидаторы
    private final UserStatusValidator validator;

    // Процессоры
    private final UserStatusPermissionProcessor permissionProcessor;
    private final UserStatusChangeProcessor changeProcessor;

    // ========== ИЗМЕНЕНИЕ СТАТУСА ==========

    @Transactional
    public User toggleUserActive(User adminUser, Long userId, boolean active) {
        try {
            // 1. Валидация администратора
            SecurityUser adminSecurity = validator.validateAdminSecurityUser(adminUser.getId(), adminUser.getEmail());

            // 2. Валидация целевого пользователя
            SecurityUser targetSecurity = validator.validateTargetSecurityUser(userId);
            User targetUser = userSearchService.getUserById(userId);

            // 3. Проверка прав
            validator.validateAdminCanManageUser(adminSecurity, targetSecurity, adminUser, targetUser);
            validator.validateNotSelfDeactivate(adminUser, targetUser, active);

            // 4. Изменение статуса
            changeProcessor.changeStatus(targetUser, targetSecurity, active, adminUser);

            return targetUser;

        } catch (Exception e) {
            log.error(messageService.get("user-status-service.log.status.change.error",
                    userId, e.getMessage()), e);
            throw e;
        }
    }

    @Transactional
    public User activateUser(User adminUser, Long userId) {
        return toggleUserActive(adminUser, userId, true);
    }

    @Transactional
    public User deactivateUser(User adminUser, Long userId) {
        return toggleUserActive(adminUser, userId, false);
    }

    // ========== ПРОВЕРКА СТАТУСА ==========

    @Transactional(readOnly = true)
    public boolean isUserActive(Long userId) {
        return permissionProcessor.isUserActive(userId);
    }

    @Transactional(readOnly = true)
    public boolean canManageUserStatus(User adminUser, Long userId) {
        return permissionProcessor.canManageUserStatus(adminUser, userId);
    }
}