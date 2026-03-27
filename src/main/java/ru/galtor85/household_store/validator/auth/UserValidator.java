package ru.galtor85.household_store.validator.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.auth.UserAlreadyExistsException;
import ru.galtor85.household_store.advice.exception.auth.UserNotFoundException;
import ru.galtor85.household_store.advice.exception.validation.ValidationRequestException;
import ru.galtor85.household_store.dto.request.user.UserEditRequest;
import ru.galtor85.household_store.dto.request.user.UserUpdatePasswordRequest;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserValidator {

    private final UserRepository userRepository;
    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;
    private final PasswordEncoder passwordEncoder;

    public void validateEmailUniqueness(String email) {
        if (email != null && userRepository.existsByEmail(email)) {
            log.warn(messageService.get("user-service.log.user.email.exists", email));
            throw new UserAlreadyExistsException(email);
        }
    }

    public void validateMobileUniqueness(String mobileNumber) {
        if (mobileNumber != null && userRepository.existsByMobileNumber(mobileNumber)) {
            log.warn(messageService.get("user-service.log.user.mobile.exists", mobileNumber));
            throw new UserAlreadyExistsException(mobileNumber);
        }
    }

    public void validateEmailUniquenessForUpdate(User user, UserEditRequest request) {
        if (request.getEmail() != null &&
                !request.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {

            log.warn(messageService.get("user-service.log.user.email.exists", request.getEmail()));
            throw new UserAlreadyExistsException(request.getEmail());
        }
    }

    public void validateMobileUniquenessForUpdate(User user, UserEditRequest request) {
        if (request.getMobileNumber() != null &&
                !request.getMobileNumber().equals(user.getMobileNumber()) &&
                userRepository.existsByMobileNumber(request.getMobileNumber())) {

            log.warn(messageService.get("user-service.log.user.mobile.exists", request.getMobileNumber()));
            throw new UserAlreadyExistsException(request.getMobileNumber());
        }
    }

    public SecurityUser validateSecurityUserExists(Long userId) {
        return securityUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-service.log.security.user.not.found", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

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

    public void validatePasswordComplexity(String password) {
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasSpecial = password.matches(".*[@#$%^&*!].*");

        if (!hasDigit || !hasLower || !hasUpper || !hasSpecial) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.new.complexity"),
                    null
            );
        }
    }
}