package ru.galtor85.household_store.security;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;

@Component
public class SecurityUserFactory {

    /**
     * Создание нового SecurityUser (для регистрации)
     */
    public SecurityUser createNew(User user, String encodedPassword, Role role) {
        return SecurityUser.builder()
                .userId(user.getId())  // Только ID, без ссылки на User
                .password(encodedPassword)
                .role(role != null ? role : Role.USER)
                .active(true)
                .build();
    }

    /**
     * Создание SecurityUser из существующего (для копирования)
     */
    public SecurityUser fromExisting(User user, SecurityUser existing) {
        return SecurityUser.builder()
                .userId(user.getId())
                .password(existing.getPassword())
                .role(existing.getRole())
                .active(existing.isActive())
                .createdAt(existing.getCreatedAt())
                .updatedAt(existing.getUpdatedAt())
                .build();
    }

    /**
     * Обновление пароля
     */
    public SecurityUser withUpdatedPassword(User user, SecurityUser existingSecurityUser, String newEncodedPassword) {
        return SecurityUser.builder()
                .userId(user.getId())
                .password(newEncodedPassword)
                .role(existingSecurityUser.getRole())
                .active(existingSecurityUser.isActive())
                .createdAt(existingSecurityUser.getCreatedAt())
                .updatedAt(existingSecurityUser.getUpdatedAt())
                .build();
    }

    /**
     * Обновление роли
     */
    public SecurityUser withUpdatedRole(User user, SecurityUser existingSecurityUser, Role newRole) {
        return SecurityUser.builder()
                .userId(user.getId())
                .password(existingSecurityUser.getPassword())
                .role(newRole)
                .active(existingSecurityUser.isActive())
                .createdAt(existingSecurityUser.getCreatedAt())
                .updatedAt(existingSecurityUser.getUpdatedAt())
                .build();
    }

    /**
     * Обновление статуса
     */
    public SecurityUser withUpdatedStatus(User user, SecurityUser existingSecurityUser, boolean active) {
        return SecurityUser.builder()
                .userId(user.getId())
                .password(existingSecurityUser.getPassword())
                .role(existingSecurityUser.getRole())
                .active(active)
                .createdAt(existingSecurityUser.getCreatedAt())
                .updatedAt(existingSecurityUser.getUpdatedAt())
                .build();
    }

    /**
     * Обновление нескольких полей сразу
     */
    public SecurityUser updateFromExisting(SecurityUser existing,
                                           String newPassword,
                                           Role newRole,
                                           Boolean newActive) {
        return SecurityUser.builder()
                .userId(existing.getUserId())  // Используем getUserId()
                .password(newPassword != null ? newPassword : existing.getPassword())
                .role(newRole != null ? newRole : existing.getRole())
                .active(newActive != null ? newActive : existing.isActive())
                .createdAt(existing.getCreatedAt())
                .updatedAt(existing.getUpdatedAt())
                .build();
    }

    /**
     * Создание SecurityUser только с ID пользователя (для случаев, когда User не нужен)
     */
    public SecurityUser createWithUserId(Long userId, String encodedPassword, Role role) {
        return SecurityUser.builder()
                .userId(userId)
                .password(encodedPassword)
                .role(role != null ? role : Role.USER)
                .active(true)
                .build();
    }
}