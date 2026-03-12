package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;

import java.time.LocalDate;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserCreationService {

    private final UserService userService;
    private final MessageService messageService;

    @Transactional
    public User createUserWithRole(User adminUser, User newUser, Role role, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        if (!adminUser.getRole().canManage(role)) {
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

        newUser.setRole(role);
        newUser.setActive(true);

        User createdUser = userService.register(newUser);

        log.info(messageService.get(
                "admin-user-creation-service.log.admin.user.created",
                adminUser.getEmail(),
                createdUser.getEmail(),
                role
        ));

        return createdUser;
    }

    @Transactional
    public User createUserWithRole(User adminUser, User newUser, Role role) {
        return createUserWithRole(adminUser, newUser, role, Locale.getDefault());
    }

    @Transactional
    public User createUserWithGeneratedPassword(User adminUser, User newUser, Role role, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        String generatedPassword = generateRandomPassword();
        newUser.setPassword(generatedPassword);

        User createdUser = createUserWithRole(adminUser, newUser, role, locale);

        log.info(messageService.get(
                "admin-user-creation-service.log.admin.user.password.generated",
                createdUser.getEmail(),
                generatedPassword
        ));

        return createdUser;
    }

    @Transactional
    public User createUserWithGeneratedPassword(User adminUser, User newUser, Role role) {
        return createUserWithGeneratedPassword(adminUser, newUser, role, Locale.getDefault());
    }

    private String generateRandomPassword() {
        // TODO: Реализовать нормальную генерацию пароля
        return "TempPass123!";
    }

    public boolean canCreateUserWithRole(User adminUser, Role role, Locale locale) {
        boolean canCreate = adminUser.getRole().canManage(role);

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