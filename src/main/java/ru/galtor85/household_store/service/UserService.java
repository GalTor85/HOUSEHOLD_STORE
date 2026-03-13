package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.ValidationRequestException;
import ru.galtor85.household_store.dto.UserEditRequest;
import ru.galtor85.household_store.dto.UserUpdatePasswordRequest;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.mapper.UserToEntity;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.repository.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.security.SecurityUserFactory;

import java.time.LocalDate;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SecurityUserRepository securityUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageService messageService;
    private final UserToEntity userToEntity;
    private final SecurityUserFactory securityUserFactory;
    private final UserSearchService userSearchService;

    @Transactional
    public User register(User user, String rawPassword, Role role, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        final String email = user.getEmail();
        final String mobileNumber = user.getMobileNumber();
        final String principal = email != null ? email : mobileNumber;

        if (email != null && userRepository.existsByEmail(email)) {
            String errorMessage = messageService.get("user-service.error.user.email.exists", email);
            log.warn(errorMessage);
            throw new ValidationRequestException(errorMessage, principal);
        }

        if (mobileNumber != null && userRepository.existsByMobileNumber(mobileNumber)) {
            String errorMessage = messageService.get("user-service.error.user.mobile.exists", mobileNumber);
            log.warn(errorMessage);
            throw new ValidationRequestException(errorMessage, principal);
        }

        User savedUser = userRepository.save(user);

        SecurityUser securityUser = securityUserFactory.createNew(
                savedUser,
                passwordEncoder.encode(rawPassword),
                role != null ? role : Role.USER
        );

        securityUserRepository.save(securityUser);

        log.info(messageService.get("user-service.log.user.newregistered", savedUser.getEmail(), savedUser.getId()));

        return savedUser;
    }

    @Transactional
    public User register(User user, String rawPassword) {
        return register(user, rawPassword, Role.USER, Locale.getDefault());
    }

    @Transactional(readOnly = true)
    public User login(String password, String value, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        SecurityUser securityUser = securityUserRepository
                .findByEmailOrMobileNumber(value)
                .orElseThrow(() -> {
                    String errorMessage = messageService.get("user-service.error.login.invalid.credentials");
                    log.warn(messageService.get("user-service.log.login.failed.not.found", value));
                    return new ValidationRequestException(errorMessage, value);
                });

        if (!passwordEncoder.matches(password, securityUser.getPassword())) {
            String errorMessage = messageService.get("user-service.error.login.invalid.credentials");
            log.warn(messageService.get("user-service.log.login.failed.wrong.password", value));
            throw new ValidationRequestException(errorMessage, value);
        }

        if (!securityUser.isEnabled()) {
            String errorMessage = messageService.get("user-service.error.login.account.deactivated");
            log.warn(messageService.get("user-service.log.login.failed.deactivated", value));
            throw new ValidationRequestException(errorMessage, value);
        }

        User user = userSearchService.getUserById(securityUser.getUserId());
        log.info(messageService.get("user-service.log.login.success", user.getEmail(), user.getId()));

        return user;
    }

    @Transactional(readOnly = true)
    public User login(String password, String value) {
        return login(password, value, Locale.getDefault());
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
    public User edit(User user, UserEditRequest request) {
        return edit(user, request, Locale.getDefault());
    }

    @Transactional
    public User passwordUpdate(User user, UserUpdatePasswordRequest request, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        final String value = user.getAuthenticationId();

        // Получаем существующий SecurityUser
        SecurityUser existingSecurityUser = securityUserRepository.findById(user.getId())
                .orElseThrow(() -> new ValidationRequestException(
                        messageService.get("user-service.error.security.user.not.found"), value));

        // Проверка текущего пароля
        if (Strings.isBlank(request.getCurrentPassword())) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.current.required"), value);
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), existingSecurityUser.getPassword())) {
            log.warn("Failed password update attempt for user {}: incorrect current password", maskEmail(user.getEmail()));
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.current.incorrect"), value);
        }

        // Проверка нового пароля
        if (Strings.isBlank(request.getNewPassword())) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.new.required"), value);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.confirm.mismatch"), value);
        }

        if (passwordEncoder.matches(request.getNewPassword(), existingSecurityUser.getPassword())) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.new.same.as.old"), value);
        }

        validatePasswordComplexity(request.getNewPassword(), locale);

        // ИСПРАВЛЕНО: передаем существующий SecurityUser в фабрику
        SecurityUser updatedSecurityUser = securityUserFactory.withUpdatedPassword(
                user,
                existingSecurityUser,  // передаем существующий SecurityUser
                passwordEncoder.encode(request.getNewPassword())
        );

        securityUserRepository.save(updatedSecurityUser);

        log.info("Password successfully updated for user: {}", maskEmail(user.getEmail()));

        return user;
    }

    @Transactional
    public User passwordUpdate(User user, UserUpdatePasswordRequest request) {
        return passwordUpdate(user, request, Locale.getDefault());
    }

    @Transactional(readOnly = true)
    public SecurityUser getSecurityUserByUserId(Long userId) {
        return securityUserRepository.findById(userId)
                .orElseThrow(() -> new ValidationRequestException(
                        messageService.get("user-service.error.security.user.not.found"), null));
    }

    private void validatePasswordComplexity(String password, Locale locale) {
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasSpecial = password.matches(".*[@#$%^&*!].*");

        if (!hasDigit || !hasLower || !hasUpper || !hasSpecial) {
            throw new ValidationRequestException(
                    messageService.get("user-service.validation.password.new.complexity"), null);
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

    private void checkEmailUniqueness(User user, UserEditRequest request, Locale locale) {
        if (request.getEmail() != null &&
                !request.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {

            throw new ValidationRequestException(
                    messageService.get("user-service.error.user.email.exists", request.getEmail()),
                    request.getEmail());
        }
    }

    private void checkMobileUniqueness(User user, UserEditRequest request, Locale locale) {
        if (request.getMobileNumber() != null &&
                !request.getMobileNumber().equals(user.getMobileNumber()) &&
                userRepository.existsByMobileNumber(request.getMobileNumber())) {

            throw new ValidationRequestException(
                    messageService.get("user-service.error.user.mobile.exists", request.getMobileNumber()),
                    request.getMobileNumber());
        }
    }
}