package ru.galtor85.household_store.security;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;

import java.time.LocalDateTime;

@Component
public class SecurityUserFactory {

    /**
     * Создание нового SecurityUser (для регистрации)
     */
    public SecurityUser createNew(User user, String encodedPassword, Role role) {
        return SecurityUser.builder()
                .userId(user.getId())
                .password(encodedPassword)
                .role(role != null ? role : Role.USER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * ✅ Обновление роли - модифицируем существующий объект
     */
    public SecurityUser withUpdatedRole(SecurityUser existingSecurityUser, Role newRole) {
        existingSecurityUser.setRole(newRole);
        existingSecurityUser.setUpdatedAt(LocalDateTime.now());
        return existingSecurityUser;
    }

    /**
     * ✅ Обновление статуса - модифицируем существующий объект
     */
    public SecurityUser withUpdatedStatus(SecurityUser existingSecurityUser, boolean active) {
        existingSecurityUser.setActive(active);
        existingSecurityUser.setUpdatedAt(LocalDateTime.now());
        return existingSecurityUser;
    }

    /**
     * ✅ Обновление пароля - модифицируем существующий объект
     */
    public SecurityUser withUpdatedPassword(SecurityUser existingSecurityUser, String newEncodedPassword) {
        existingSecurityUser.setPassword(newEncodedPassword);
        existingSecurityUser.setUpdatedAt(LocalDateTime.now());
        return existingSecurityUser;
    }

    /**
     * ✅ Обновление нескольких полей сразу
     */
    public SecurityUser updateFromExisting(SecurityUser existing,
                                           String newPassword,
                                           Role newRole,
                                           Boolean newActive) {
        if (newPassword != null) {
            existing.setPassword(newPassword);
        }
        if (newRole != null) {
            existing.setRole(newRole);
        }
        if (newActive != null) {
            existing.setActive(newActive);
        }
        existing.setUpdatedAt(LocalDateTime.now());
        return existing;
    }
}