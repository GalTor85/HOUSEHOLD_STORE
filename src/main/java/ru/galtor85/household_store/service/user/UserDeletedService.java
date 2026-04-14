package ru.galtor85.household_store.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.entity.user.UserTypeAssignment;
import ru.galtor85.household_store.processor.delete.HardDeleteProcessor;
import ru.galtor85.household_store.processor.delete.SoftDeleteProcessor;
import ru.galtor85.household_store.repository.user.UserTypeAssignmentRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.validator.auth.UserDeleteValidator;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletedService {



    // Валидаторы
    private final UserDeleteValidator validator;

    // Процессоры
    private final HardDeleteProcessor hardDeleteProcessor;
    private final SoftDeleteProcessor softDeleteProcessor;


    private final UserTypeAssignmentRepository userTypeAssignmentRepository;
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

        List<UserTypeAssignment> assignments = userTypeAssignmentRepository.findByUserId(userId);
        if (!assignments.isEmpty()) {
            userTypeAssignmentRepository.deleteAll(assignments);
        }

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