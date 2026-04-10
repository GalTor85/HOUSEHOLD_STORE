package ru.galtor85.household_store.validator.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.auth.UserAlreadyExistsException;
import ru.galtor85.household_store.advice.exception.auth.UserNotFoundException;
import ru.galtor85.household_store.advice.exception.validation.ValidationRequestException;
import ru.galtor85.household_store.config.BusinessConfig;
import ru.galtor85.household_store.dto.request.user.UserEditRequest;
import ru.galtor85.household_store.dto.request.user.UserUpdatePasswordRequest;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.util.regex.Pattern;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Validator for user-related operations.
 *
 * <p>This validator provides comprehensive validation for user data including:</p>
 * <ul>
 *   <li>Email and mobile number uniqueness</li>
 *   <li>Password complexity and strength requirements</li>
 *   <li>Name, email, and phone number length constraints</li>
 *   <li>Current password verification for password changes</li>
 *   <li>Security user existence validation</li>
 * </ul>
 *
 * <p>All validation rules are configurable through {@link BusinessConfig}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserValidator {

    private final UserRepository userRepository;
    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;
    private final PasswordEncoder passwordEncoder;
    private final BusinessConfig businessConfig;

    // =========================================================================
    // UNIQUENESS VALIDATION
    // =========================================================================

    /**
     * Validates that an email address is unique (not already used by another user).
     * Also validates email length before checking existence.
     *
     * @param email the email address to validate
     * @throws UserAlreadyExistsException if email is already registered
     * @throws ValidationRequestException if email exceeds maximum allowed length
     */
    public void validateEmailUniqueness(String email) {
        if (email != null) {
            validateEmailLength(email);
            if (userRepository.existsByEmail(email)) {
                log.warn(messageService.get("user-service.log.user.email.exists", email));
                throw new UserAlreadyExistsException(email);
            }
        }
    }

    /**
     * Validates that a mobile number is unique (not already used by another user).
     * Also validates phone number length before checking existence.
     *
     * @param mobileNumber the mobile number to validate
     * @throws UserAlreadyExistsException if mobile number is already registered
     * @throws ValidationRequestException if phone number exceeds maximum allowed length
     */
    public void validateMobileUniqueness(String mobileNumber) {
        if (mobileNumber != null) {
            validatePhoneNumberLength(mobileNumber);
            if (userRepository.existsByMobileNumber(mobileNumber)) {
                log.warn(messageService.get("user-service.log.user.mobile.exists", mobileNumber));
                throw new UserAlreadyExistsException(mobileNumber);
            }
        }
    }

    /**
     * Validates email uniqueness during user profile update.
     * Skips validation if email hasn't changed.
     *
     * @param user    the existing user
     * @param request the update request containing new email
     * @throws UserAlreadyExistsException if new email is already used by another user
     */
    public void validateEmailUniquenessForUpdate(User user, UserEditRequest request) {
        if (request.getEmail() != null &&
                !request.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {

            log.warn(messageService.get("user-service.log.user.email.exists", request.getEmail()));
            throw new UserAlreadyExistsException(request.getEmail());
        }
    }

    /**
     * Validates mobile number uniqueness during user profile update.
     * Skips validation if mobile number hasn't changed.
     *
     * @param user    the existing user
     * @param request the update request containing new mobile number
     * @throws UserAlreadyExistsException if new mobile number is already used by another user
     */
    public void validateMobileUniquenessForUpdate(User user, UserEditRequest request) {
        if (request.getMobileNumber() != null &&
                !request.getMobileNumber().equals(user.getMobileNumber()) &&
                userRepository.existsByMobileNumber(request.getMobileNumber())) {

            log.warn(messageService.get("user-service.log.user.mobile.exists", request.getMobileNumber()));
            throw new UserAlreadyExistsException(request.getMobileNumber());
        }
    }

    // =========================================================================
    // SECURITY USER VALIDATION
    // =========================================================================

    /**
     * Validates that a security user exists for the given user ID.
     *
     * @param userId the user ID
     * @return the SecurityUser entity
     * @throws UserNotFoundException if no security user found for the given ID
     */
    public SecurityUser validateSecurityUserExists(Long userId) {
        return securityUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-service.log.security.user.not.found", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    // =========================================================================
    // PASSWORD VALIDATION
    // =========================================================================

    /**
     * Validates the current password during password change operation.
     *
     * @param currentPassword the password provided by user
     * @param encodedPassword the stored encoded password
     * @param value           the user identifier (for error context)
     * @throws ValidationRequestException if current password is empty or incorrect
     */
    public void validateCurrentPassword(String currentPassword, String encodedPassword, String value) {
        if (Strings.isBlank(currentPassword)) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.current.required"),
                    value
            );
        }

        if (!passwordEncoder.matches(currentPassword, encodedPassword)) {
            log.warn("Failed password update attempt: incorrect current password");
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.current.incorrect"),
                    value
            );
        }
    }

    /**
     * Validates the new password during password change operation.
     * Checks that new password is not empty, matches confirmation,
     * and is not the same as the old password.
     *
     * @param request                the password update request
     * @param currentEncodedPassword the stored encoded current password
     * @param value                  the user identifier (for error context)
     * @throws ValidationRequestException if validation fails
     */
    public void validateNewPassword(UserUpdatePasswordRequest request, String currentEncodedPassword,
                                    String value) {
        if (Strings.isBlank(request.getNewPassword())) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.new.required"),
                    value
            );
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.confirm.mismatch"),
                    value
            );
        }

        if (passwordEncoder.matches(request.getNewPassword(), currentEncodedPassword)) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.new.same.as.old"),
                    value
            );
        }
    }

    /**
     * Validates password complexity requirements.
     * Password must contain at least one digit, one lowercase letter,
     * one uppercase letter, and one special character.
     *
     * @param password the password to validate
     * @throws ValidationRequestException if password does not meet complexity requirements
     */
    public void validatePasswordComplexity(String password) {
        Integer minLength = businessConfig.getUser().getMinPasswordLength();

        if (minLength != null && password.length() < minLength) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.length", minLength),
                    null
            );
        }

        boolean hasDigit = password.matches(CONTAINS_DIGIT);
        boolean hasLower = password.matches(CONTAINS_LOWERCASE);
        boolean hasUpper = password.matches(CONTAINS_UPPERCASE);

        String allowedSpecialChars = businessConfig.getUser().getAllowedSpecialChars();
        String specialRegex = ".*[" + Pattern.quote(allowedSpecialChars) + "].*";
        boolean hasSpecial = password.matches(specialRegex);

        if (!hasDigit || !hasLower || !hasUpper || !hasSpecial) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.new.complexity"),
                    null
            );
        }
    }

    // =========================================================================
    // FIELD LENGTH VALIDATION
    // =========================================================================

    /**
     * Validates first name length requirements.
     * Length constraints are configured in BusinessConfig.
     *
     * @param firstName the first name to validate
     * @throws ValidationRequestException if first name does not meet length requirements
     */
    public void validateFirstName(String firstName) {
        if (firstName == null) {
            return;
        }

        Integer minLength = businessConfig.getUser().getMinNameLength();
        Integer maxLength = businessConfig.getUser().getMaxNameLength();

        if (minLength != null && firstName.length() < minLength) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.firstname.min.length", minLength),
                    null
            );
        }

        if (maxLength != null && firstName.length() > maxLength) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.firstname.max.length", maxLength),
                    null
            );
        }
    }

    /**
     * Validates last name length requirements.
     * Length constraints are configured in BusinessConfig.
     *
     * @param lastName the last name to validate
     * @throws ValidationRequestException if last name does not meet length requirements
     */
    public void validateLastName(String lastName) {
        if (lastName == null) {
            return;
        }

        Integer minLength = businessConfig.getUser().getMinNameLength();
        Integer maxLength = businessConfig.getUser().getMaxNameLength();

        if (minLength != null && lastName.length() < minLength) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.lastname.min.length", minLength),
                    null
            );
        }

        if (maxLength != null && lastName.length() > maxLength) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.lastname.max.length", maxLength),
                    null
            );
        }
    }

    /**
     * Validates email length requirements.
     * Maximum length is configured in BusinessConfig.
     *
     * @param email the email to validate
     * @throws ValidationRequestException if email exceeds maximum allowed length
     */
    public void validateEmailLength(String email) {
        if (email == null) {
            return;
        }

        Integer maxLength = businessConfig.getUser().getMaxEmailLength();

        if (maxLength != null && email.length() > maxLength) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.email.max.length", maxLength),
                    null
            );
        }
    }

    /**
     * Validates phone number length requirements.
     * Maximum length is configured in BusinessConfig.
     *
     * @param phoneNumber the phone number to validate
     * @throws ValidationRequestException if phone number exceeds maximum allowed length
     */
    public void validatePhoneNumberLength(String phoneNumber) {
        if (phoneNumber == null) {
            return;
        }

        String cleaned = phoneNumber.replaceAll(KEEP_ONLY_DIGITS, "");

        if (cleaned.length() < MIN_PHONE_LENGTH) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.phone.min.length", MIN_PHONE_LENGTH),
                    phoneNumber
            );
        }

        Integer maxLength = businessConfig.getUser().getMaxPhoneLength();

        if (maxLength != null && cleaned.length() > maxLength) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.phone.max.length", maxLength),
                    null
            );
        }
    }

    /**
     * Validates address length requirements.
     * Maximum length is configured in BusinessConfig.
     *
     * @param address the address to validate
     * @throws ValidationRequestException if address exceeds maximum allowed length
     */
    public void validateAddressLength(String address) {
        if (address == null) {
            return;
        }

        Integer maxLength = businessConfig.getUser().getMaxAddressLength();

        if (maxLength != null && address.length() > maxLength) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.address.max.length", maxLength),
                    null
            );
        }
    }

    /**
     * Validates surname (patronymic) length requirements.
     * Maximum length is configured in BusinessConfig.
     *
     * @param surname the surname to validate
     * @throws ValidationRequestException if surname exceeds maximum allowed length
     */
    public void validateSurnameLength(String surname) {
        if (surname == null) {
            return;
        }

        Integer maxLength = businessConfig.getUser().getMaxSurnameLength();

        if (maxLength != null && surname.length() > maxLength) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.surname.max.length", maxLength),
                    null
            );
        }
    }
}