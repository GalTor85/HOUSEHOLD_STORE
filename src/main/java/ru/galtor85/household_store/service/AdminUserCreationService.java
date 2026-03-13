package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;

import java.time.LocalDate;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserCreationService {

    private final UserService userService;
    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;

    @Transactional
    public User createUserWithRole(User adminUser, User newUser, String rawPassword, Role role, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        // Проверяем права администратора
        SecurityUser adminSecurity = securityUserRepository.findById(adminUser.getId())
                .orElseThrow(() -> {
                    String error = messageService.get(
                            "admin-user-creation-service.error.admin.security.not.found",
                            adminUser.getEmail()
                    );
                    log.error(error);
                    return new AccessDeniedException(error);
                });

        if (!adminSecurity.getRole().canManage(role)) {
            String errorMessage = messageService.get(
                    "admin-user-creation-service.error.admin.insufficient.rights.create",
                    role
            );
            log.warn(messageService.get(
                    "admin-user-creation-service.log.admin.insufficient.rights.create",
                    adminUser.getEmail(),
                    role
            ));
            throw new AccessDeniedException(errorMessage);
        }

        if (newUser.getBirthDate() == null) {
            newUser.setBirthDate(LocalDate.now().minusYears(18));
            log.debug(messageService.get(
                    "admin-user-creation-service.log.user.birthdate.default",
                    18
            ));
        }

        // Создаем пользователя через существующий сервис с указанной ролью
        User createdUser = userService.register(newUser, rawPassword, role, locale);

        log.info(messageService.get(
                "admin-user-creation-service.log.admin.user.created",
                adminUser.getEmail(),
                createdUser.getEmail(),
                role
        ));

        return createdUser;
    }

    @Transactional
    public User createUserWithRole(User adminUser, User newUser, String rawPassword, Role role) {
        return createUserWithRole(adminUser, newUser, rawPassword, role, Locale.getDefault());
    }

    @Transactional
    public User createUserWithGeneratedPassword(User adminUser, User newUser, Role role, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        String generatedPassword = generateRandomPassword();
        log.info(messageService.get(
                "admin-user-creation-service.log.password.generated",
                newUser.getEmail()
        ));

        return createUserWithRole(adminUser, newUser, generatedPassword, role, locale);
    }

    @Transactional
    public User createUserWithGeneratedPassword(User adminUser, User newUser, Role role) {
        return createUserWithGeneratedPassword(adminUser, newUser, role, Locale.getDefault());
    }

    private String generateRandomPassword() {
        // TODO: Реализовать нормальную генерацию пароля
        return "TempPass123!";
    }

    @Transactional(readOnly = true)
    public boolean canCreateUserWithRole(User adminUser, Role role, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        SecurityUser adminSecurity = securityUserRepository.findById(adminUser.getId())
                .orElseThrow(() -> {
                    String error = messageService.get(
                            "admin-user-creation-service.error.admin.security.not.found",
                            adminUser.getEmail()
                    );
                    log.error(error);
                    return new AccessDeniedException(error);
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

    @Transactional(readOnly = true)
    public boolean canCreateUserWithRole(User adminUser, Role role) {
        return canCreateUserWithRole(adminUser, role, Locale.getDefault());
    }
}