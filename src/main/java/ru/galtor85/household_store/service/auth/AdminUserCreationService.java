package ru.galtor85.household_store.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.auth.UserAccessException;
import ru.galtor85.household_store.advice.exception.auth.UserNotFoundException;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.email.EmailMasker;

import java.time.LocalDate;

/**
 * Service for creating users with administrative privileges.
 *
 * <p>Handles user creation by administrators with role-based access control.
 * Administrators can only create users with roles they are permitted to manage
 * according to the role hierarchy.</p>
 *
 * <p><b>Role hierarchy:</b> ADMIN > MANAGER > USER</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserCreationService {

    private static final int DEFAULT_AGE_YEARS = 18;

    private final UserService userService;
    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;
    private final EmailMasker emailMasker;

    /**
     * Creates a new user with a specified role.
     *
     * <p>Performs the following checks and actions:
     * <ol>
     *   <li>Verifies the admin has permission to assign the requested role</li>
     *   <li>Sets a default birthdate (18 years ago) if not provided</li>
     *   <li>Registers the user through the main UserService</li>
     * </ol>
     *
     * @param adminUser the administrator creating the user
     * @param newUser the user entity to create
     * @param rawPassword the raw (unencoded) password
     * @param role the role to assign to the new user
     * @return the created User entity
     * @throws UserNotFoundException if admin security user not found
     * @throws UserAccessException if admin lacks permission to assign the role
     */
    @Transactional
    public User createUserWithRole(User adminUser, User newUser, String rawPassword, Role role) {
        String maskedAdminEmail = emailMasker.maskEmail(adminUser.getEmail());

        validateAdminPermissions(adminUser, role);

        setDefaultBirthDateIfNeeded(newUser);

        User createdUser = userService.register(newUser, rawPassword, role);

        log.info(logMsg.get(
                "admin-user-creation-service.log.admin.user.created",
                maskedAdminEmail,
                emailMasker.maskEmail(createdUser.getEmail()),
                role
        ));

        return createdUser;
    }

    /**
     * Validates that the admin user exists and has permission to assign the role.
     *
     * @param adminUser the administrator user
     * @param role      the role to assign
     * @throws UserNotFoundException if admin security user not found
     * @throws UserAccessException   if admin lacks permission
     */
    private void validateAdminPermissions(User adminUser, Role role) {
        SecurityUser adminSecurity = securityUserRepository.findById(adminUser.getId())
                .orElseThrow(() -> {
                    String error = messageService.get(
                            "admin-user-creation-service.error.admin.security.not.found",
                            emailMasker.maskEmail(adminUser.getEmail())
                    );
                    log.error(error);
                    return new UserNotFoundException(adminUser.getId().toString());
                });

        if (!adminSecurity.getRole().canManage(role)) {
            log.warn(logMsg.get(
                    "admin-user-creation-service.log.admin.insufficient.rights.create",
                    emailMasker.maskEmail(adminUser.getEmail()),
                    role
            ));
            throw new UserAccessException(
                    messageService.get("admin-user-creation-service.error.admin.insufficient.rights.create", role)
            );
        }

    }

    /**
     * Sets a default birthdate if none was provided.
     *
     * @param newUser the user entity to update
     */
    private void setDefaultBirthDateIfNeeded(User newUser) {
        if (newUser.getBirthDate() == null) {
            newUser.setBirthDate(LocalDate.now().minusYears(DEFAULT_AGE_YEARS));
            log.debug(logMsg.get(
                    "admin-user-creation-service.log.user.birthdate.default",
                    DEFAULT_AGE_YEARS
            ));
        }
    }
}