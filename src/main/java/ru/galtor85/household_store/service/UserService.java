package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.ValidationRequestException;
import ru.galtor85.household_store.dto.UserCreateRequest;
import ru.galtor85.household_store.dto.UserEditRequest;
import ru.galtor85.household_store.dto.UserUpdatePasswordRequest;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.mapper.UserToEntity;
import ru.galtor85.household_store.repository.UserRepository;

import java.time.LocalDate;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageService messageService;
    private final UserToEntity userToEntity;

    @Transactional
    public User register(User user, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        final Locale finalLocale = locale;
        final String email = user.getEmail();
        final String mobileNumber = user.getMobileNumber();
        final String principal = email != null ? email : mobileNumber;

        if (email != null && userRepository.existsByEmail(email)) {
            String errorMessage = messageService.get(
                    "user-service.error.user.email.exists",
                    email
            );
            log.warn(errorMessage);
            throw new ValidationRequestException(errorMessage, principal);
        }

        if (mobileNumber != null && userRepository.existsByMobileNumber(mobileNumber)) {
            String errorMessage = messageService.get(
                    "user-service.error.user.mobile.exists",
                    mobileNumber
            );
            log.warn(errorMessage);
            throw new ValidationRequestException(errorMessage, principal);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);

        log.info(messageService.get(
                "user-service.log.user.newregistered",
                savedUser.getEmail(),
                savedUser.getId()
        ));

        return savedUser;
    }

    @Transactional(readOnly = true)
    public User login(String password, String value, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        final Locale finalLocale = locale;
        final String finalValue = value;

        User user = userRepository.findByEmailOrMobileNumber(value, value)
                .orElseThrow(() -> {
                    String errorMessage = messageService.get(
                            "user-service.error.login.invalid.credentials"
                    );
                    log.warn(messageService.get(
                            "user-service.log.login.failed.not.found",
                            finalValue
                    ));
                    return new ValidationRequestException(errorMessage, finalValue);
                });

        if (!passwordEncoder.matches(password, user.getPassword())) {
            String errorMessage = messageService.get(
                    "user-service.error.login.invalid.credentials"
            );
            log.warn(messageService.get(
                    "user-service.log.login.failed.wrong.password",
                    value
            ));
            throw new ValidationRequestException(errorMessage, value);
        }

        if (!user.isActive()) {
            String errorMessage = messageService.get(
                    "user-service.error.login.account.deactivated"
            );
            log.warn(messageService.get(
                    "user-service.log.login.failed.deactivated",
                    value
            ));
            throw new ValidationRequestException(errorMessage, value);
        }

        log.info(messageService.get(
                "user-service.log.login.success",
                user.getEmail(),
                user.getId()
        ));

        return user;
    }

    @Transactional
    public User edit(User user, UserEditRequest request, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        checkEmailUniqueness(user, request, locale);
        checkMobileUniqueness(user, request, locale);

        userToEntity.updateUserFromRequest(user, request, locale);

        return user;
    }

    @Transactional
    public User passwordUpdate(User user, UserUpdatePasswordRequest request, Locale locale) {

        final Locale finalLocale = locale;
        final String value = user.getEmail() != null ? user.getEmail() : user.getMobileNumber();
        locale = locale != null ? locale : Locale.getDefault();

        if (Strings.isBlank(request.getCurrentPassword())) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.current.required"),
                    value
            );
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Failed password update attempt for user {}: incorrect current password",
                    maskEmail(user.getEmail()));
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.current.incorrect"),
                    value
            );
        }

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

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.new.same.as.old"),
                    value
            );
        }

        validatePasswordComplexity(request.getNewPassword(), locale);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        log.info("Password successfully updated for user: {}", maskEmail(user.getEmail()));

        return user;
    }

    private void validatePasswordComplexity(String password, Locale locale) {
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

    private String maskEmail(String email) {
        if (email == null) return "null";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email;

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        return localPart.charAt(0) + "***" +
                (localPart.length() > 2 ? localPart.charAt(localPart.length() - 1) : "") +
                domain;
    }

    private boolean noChanges(UserCreateRequest request) {
        return Strings.isBlank(request.getFirstName()) &&
                Strings.isBlank(request.getLastName()) &&
                Strings.isBlank(request.getSurname()) &&
                Strings.isBlank(request.getEmail()) &&
                Strings.isBlank(request.getAddress()) &&
                Strings.isBlank(request.getBirthDate()) &&
                Strings.isBlank(request.getMobileNumber()) &&
                Strings.isBlank(request.getPassword());
    }

    private void updateUserFields(User user, UserCreateRequest request, Locale locale) {
        if (Strings.isNotBlank(request.getFirstName())) {
            user.setFirstName(request.getFirstName());
        }
        if (Strings.isNotBlank(request.getLastName())) {
            user.setLastName(request.getLastName());
        }
        if (Strings.isNotBlank(request.getSurname())) {
            user.setSurname(request.getSurname());
        }
        if (Strings.isNotBlank(request.getAddress())) {
            user.setAddress(request.getAddress());
        }
        if (Strings.isNotBlank(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (Strings.isNotBlank(request.getMobileNumber())) {
            user.setMobileNumber(request.getMobileNumber());
        }
        if (request.getBirthDate() != null) {
            try {
                user.setBirthDate(LocalDate.parse(request.getBirthDate()));
            } catch (Exception e) {
                String errorMessage = messageService.get(
                        "user-service.error.user.birthdate.invalid",
                        request.getBirthDate()
                );
                throw new ValidationRequestException(errorMessage,
                        user.getEmail() != null ? user.getEmail() : user.getMobileNumber());
            }
        }
    }

    // ========== ПЕРЕГРУЖЕННЫЕ МЕТОДЫ ДЛЯ ОБРАТНОЙ СОВМЕСТИМОСТИ ==========

    @Transactional
    public User register(User user) {
        return register(user, Locale.getDefault());
    }

    @Transactional
    public User passwordUpdate(User user, UserUpdatePasswordRequest request) {
        return passwordUpdate(user, request, Locale.getDefault());
    }

    @Transactional(readOnly = true)
    public User login(String password, String value) {
        return login(password, value, Locale.getDefault());
    }

    @Transactional
    public User edit(User user, UserEditRequest request) {
        return edit(user, request, Locale.getDefault());
    }

    private void checkEmailUniqueness(User user, UserEditRequest request, Locale locale) {
        if (request.getEmail() != null &&
                !request.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {

            throw new ValidationRequestException(
                    messageService.get("user-service.error.user.email.exists", request.getEmail()),
                    request.getEmail()
            );
        }
    }

    private void checkMobileUniqueness(User user, UserEditRequest request, Locale locale) {
        if (request.getMobileNumber() != null &&
                !request.getMobileNumber().equals(user.getMobileNumber()) &&
                userRepository.existsByMobileNumber(request.getMobileNumber())) {

            throw new ValidationRequestException(
                    messageService.get("user-service.error.user.mobile.exists", request.getMobileNumber()),
                    request.getMobileNumber()
            );
        }
    }
}