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
import ru.galtor85.household_store.processor.user.UserLoginProcessor;
import ru.galtor85.household_store.processor.user.UserPasswordUpdateProcessor;
import ru.galtor85.household_store.processor.user.UserRegistrationProcessor;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.email.EmailMasker;
import ru.galtor85.household_store.validator.auth.UserValidator;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final MessageService messageService;
    private final UserToEntity userToEntity;

    // Валидаторы
    private final UserValidator validator;

    // Процессоры
    private final UserRegistrationProcessor registrationProcessor;
    private final UserLoginProcessor loginProcessor;
    private final UserPasswordUpdateProcessor passwordUpdateProcessor;

    // Утилиты
    private final EmailMasker emailMasker;

    // ========== РЕГИСТРАЦИЯ ==========

    @Transactional
    public User register(User user, String rawPassword, Role role) {
        final String email = user.getEmail();
        final String mobileNumber = user.getMobileNumber();

        // Валидация уникальности
        validator.validateEmailUniqueness(email);
        validator.validateMobileUniqueness(mobileNumber);

        // Регистрация
        return registrationProcessor.register(user, rawPassword, role);
    }

    @Transactional
    public User register(User user, String rawPassword) {
        return register(user, rawPassword, Role.USER);
    }

    // ========== ЛОГИН ==========

    @Transactional(readOnly = true)
    public User login(String password, String value) {
        return loginProcessor.login(password, value);
    }

    // ========== РЕДАКТИРОВАНИЕ ПРОФИЛЯ ==========

    @Transactional
    public User edit(User user, UserEditRequest request) {
        // Валидация уникальности при изменении
        validator.validateEmailUniquenessForUpdate(user, request);
        validator.validateMobileUniquenessForUpdate(user, request);

        // Обновление через маппер
        userToEntity.updateUserFromRequest(user, request);

        return user;
    }

    // ========== ОБНОВЛЕНИЕ ПАРОЛЯ ==========

    @Transactional
    public User passwordUpdate(User user, UserUpdatePasswordRequest request) {
        final String value = user.getAuthenticationId();

        // Получаем существующий SecurityUser
        SecurityUser existingSecurityUser = validator.validateSecurityUserExists(user.getId());

        // Валидация текущего пароля
        validator.validateCurrentPassword(request.getCurrentPassword(),
                existingSecurityUser.getPassword(), value);

        // Валидация нового пароля
        validator.validateNewPassword(request, existingSecurityUser.getPassword(), value);

        // Валидация сложности пароля
        validator.validatePasswordComplexity(request.getNewPassword());

        // Обновление пароля
        passwordUpdateProcessor.updatePassword(user, existingSecurityUser, request);

        log.info(messageService.get("user-service.log.password.updated",
                emailMasker.maskEmail(user.getEmail())));

        return user;
    }

    // ========== ПОЛУЧЕНИЕ SECURITY USER ==========

    @Transactional(readOnly = true)
    public SecurityUser getSecurityUserByUserId(Long userId) {
        return validator.validateSecurityUserExists(userId);
    }
}