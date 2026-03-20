package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.processor.HardDeleteProcessor;
import ru.galtor85.household_store.processor.SoftDeleteProcessor;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.validator.UserDeleteValidator;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletedService {

    // Валидаторы
    private final UserDeleteValidator validator;

    // Процессоры
    private final HardDeleteProcessor hardDeleteProcessor;
    private final SoftDeleteProcessor softDeleteProcessor;

    // Утилиты
    //private final EmailAnonymizer emailAnonymizer; TODO

    // ========== ЖЕСТКОЕ УДАЛЕНИЕ ==========

    @Transactional
    public void deleteUser(Long userId) {
        // Валидация
        User user = validator.validateUserExists(userId);
        validator.validateSecurityUserExists(userId); // Проверяем, что security запись существует

        // Удаление
        hardDeleteProcessor.deleteUser(user, userId);
    }

    @Transactional
    public void deleteUserWithCheck(Long userId, User adminUser) {
        // Валидация существования
        User userToDelete = validator.validateUserExists(userId);
        SecurityUser targetSecurity = validator.validateSecurityUserExists(userId);
        SecurityUser adminSecurity = validator.validateAdminSecurityUserExists(adminUser.getId(), adminUser.getEmail());

        // Проверки прав
        validator.validateNotSelfDelete(adminUser.getId(), userId);
        validator.validateAdminRights(adminSecurity, targetSecurity);

        // Удаление
        hardDeleteProcessor.deleteUserByAdmin(userToDelete, adminUser, userId);
    }

    // ========== МЯГКОЕ УДАЛЕНИЕ ==========

    @Transactional
    public void softDeleteUser(Long userId) {
        // Валидация
        User user = validator.validateUserExists(userId);
        SecurityUser securityUser = validator.validateSecurityUserExists(userId);

        // Мягкое удаление
        softDeleteProcessor.softDeleteUser(user, securityUser);
    }
}