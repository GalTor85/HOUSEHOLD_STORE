package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserCreationService {

    private final UserService userService;
    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;

    @Transactional
    public User createUserWithRole(User adminUser, User newUser, String rawPassword, Role role) {
        // Проверяем права администратора
        SecurityUser adminSecurity = securityUserRepository.findById(adminUser.getId())
                .orElseThrow(() -> {
                    String error = messageService.get(
                            "admin-user-creation-service.error.admin.security.not.found",
                            adminUser.getEmail()
                    );
                    log.error(error);
                    return new UserNotFoundException(adminUser.getId().toString());
                });

        if (!adminSecurity.getRole().canManage(role)) {
            log.warn(messageService.get(
                    "admin-user-creation-service.log.admin.insufficient.rights.create",
                    adminUser.getEmail(),
                    role
            ));
            throw new UserAccessException(
                    messageService.get("admin-user-creation-service.error.admin.insufficient.rights.create", role)
            );
        }

        if (newUser.getBirthDate() == null) {
            newUser.setBirthDate(LocalDate.now().minusYears(18));
            log.debug(messageService.get(
                    "admin-user-creation-service.log.user.birthdate.default",
                    18
            ));
        }

        // Создаем пользователя через существующий сервис с указанной ролью
        User createdUser = userService.register(newUser, rawPassword, role);

        log.info(messageService.get(
                "admin-user-creation-service.log.admin.user.created",
                adminUser.getEmail(),
                createdUser.getEmail(),
                role
        ));

        return createdUser;
    }

    @Transactional
    public User createUserWithGeneratedPassword(User adminUser, User newUser, Role role) {
        String generatedPassword = generateRandomPassword();
        log.info(messageService.get(
                "admin-user-creation-service.log.password.generated",
                newUser.getEmail()
        ));

        return createUserWithRole(adminUser, newUser, generatedPassword, role);
    }

    private String generateRandomPassword() {
        // TODO: Реализовать нормальную генерацию пароля
        return "TempPass123!";
    }

    @Transactional(readOnly = true)
    public boolean canCreateUserWithRole(User adminUser, Role role) {
        SecurityUser adminSecurity = securityUserRepository.findById(adminUser.getId())
                .orElseThrow(() -> {
                    String error = messageService.get(
                            "admin-user-creation-service.error.admin.security.not.found",
                            adminUser.getEmail()
                    );
                    log.error(error);
                    return new UserNotFoundException(adminUser.getId().toString());
                });

        boolean canCreate = adminSecurity.getRole().canManage(role);

        if (!canCreate) {
            log.debug(messageService.get(
                    "admin-user-creation-service.log.admin.cannot.create.role",
                    adminUser.getEmail(),
                    role
            ));
        }

        return canCreate;
    }
}