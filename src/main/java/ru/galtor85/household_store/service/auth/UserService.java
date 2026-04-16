package ru.galtor85.household_store.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.request.user.UserEditRequest;
import ru.galtor85.household_store.dto.request.user.UserUpdatePasswordRequest;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.mapper.user.UserToEntity;
import ru.galtor85.household_store.processor.user.UserPasswordUpdateProcessor;
import ru.galtor85.household_store.processor.user.UserRegistrationProcessor;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.util.email.EmailMasker;
import ru.galtor85.household_store.validator.auth.UserValidator;

/**
 * Service for managing user accounts.
 *
 * <p>Provides core user management functionality including registration,
 * profile editing, and password updates. All operations are transactional
 * and include proper validation.</p>
 *
 * @author G@LTor85
 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final LogMessageService logMsg;
    private final UserToEntity userToEntity;
    private final UserValidator validator;
    private final UserRegistrationProcessor registrationProcessor;
    private final UserPasswordUpdateProcessor passwordUpdateProcessor;
    private final EmailMasker emailMasker;

    /**
     * Registers a new user with a specified role.
     *
     * <p>Validates email and mobile number uniqueness before registration.</p>
     *
     * @param user the user entity to register
     * @param rawPassword the raw (unencoded) password
     * @param role the role to assign to the new user
     * @return the registered User entity
     * @throws ru.galtor85.household_store.advice.exception.auth.UserAlreadyExistsException if email or mobile already exists
     */
    @Transactional
    public User register(User user, String rawPassword, Role role) {
        validator.validateEmailUniqueness(user.getEmail());
        validator.validateMobileUniqueness(user.getMobileNumber());
        return registrationProcessor.register(user, rawPassword, role);
    }

    /**
     * Registers a new user with the default USER role.
     *
     * @param user the user entity to register
     * @param rawPassword the raw (unencoded) password
     * @return the registered User entity
     */
    @Transactional
    public User register(User user, String rawPassword) {
        return register(user, rawPassword, Role.USER);
    }

    /**
     * Edits an existing user's profile.
     *
     * <p>Validates uniqueness of email and mobile number if they are being changed.</p>
     *
     * @param user the existing user entity
     * @param request the edit request containing updated fields
     * @return the updated User entity
     */
    @Transactional
    public User edit(User user, UserEditRequest request) {
        validator.validateEmailUniquenessForUpdate(user, request);
        validator.validateMobileUniquenessForUpdate(user, request);
        userToEntity.updateUserFromRequest(user, request);
        return user;
    }

    /**
     * Updates a user's password.
     *
     * <p>Performs comprehensive validation before updating:
     * <ul>
     *   <li>Verifies the current password is correct</li>
     *   <li>Ensures new password matches confirmation</li>
     *   <li>Validates password complexity requirements</li>
     *   <li>Ensures new password is different from current</li>
     * </ul>
     *
     * @param user the user whose password is being updated
     * @param request the password update request containing current and new passwords
     * @return the User entity (unchanged)
     * @throws ru.galtor85.household_store.advice.exception.validation.ValidationRequestException if validation fails
     */
    @Transactional
    public User passwordUpdate(User user, UserUpdatePasswordRequest request) {
        String identifier = user.getAuthenticationId();
        SecurityUser existingSecurityUser = validator.validateSecurityUserExists(user.getId());

        validator.validateCurrentPassword(request.getCurrentPassword(),
                existingSecurityUser.getPassword(), identifier);
        validator.validateNewPassword(request, existingSecurityUser.getPassword(), identifier);
        validator.validatePasswordComplexity(request.getNewPassword());

        passwordUpdateProcessor.updatePassword(user, existingSecurityUser, request);

        log.info(logMsg.get("user-service.log.password.updated",
                emailMasker.maskEmail(user.getEmail())));

        return user;
    }
}