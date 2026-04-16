package ru.galtor85.household_store.security;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;

import java.time.LocalDateTime;

/**
 * Factory for creating and updating SecurityUser entities.
 */
@Component
public class SecurityUserFactory {

    /**
     * Creates new SecurityUser for registration.
     *
     * @param user domain user entity
     * @param encodedPassword encoded password
     * @param role security role
     * @return new SecurityUser entity
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
     * Updates role on existing SecurityUser.
     *
     * @param existingSecurityUser existing security user
     * @param newRole new role to assign
     * @return updated SecurityUser
     */
    public SecurityUser withUpdatedRole(SecurityUser existingSecurityUser, Role newRole) {
        existingSecurityUser.setRole(newRole);
        existingSecurityUser.setUpdatedAt(LocalDateTime.now());
        return existingSecurityUser;
    }

    /**
     * Updates active status on existing SecurityUser.
     *
     * @param existingSecurityUser existing security user
     * @param active new active status
     * @return updated SecurityUser
     */
    public SecurityUser withUpdatedStatus(SecurityUser existingSecurityUser, boolean active) {
        existingSecurityUser.setActive(active);
        existingSecurityUser.setUpdatedAt(LocalDateTime.now());
        return existingSecurityUser;
    }

    /**
     * Updates password on existing SecurityUser.
     *
     * @param existingSecurityUser existing security user
     * @param newEncodedPassword new encoded password
     * @return updated SecurityUser
     */
    public SecurityUser withUpdatedPassword(SecurityUser existingSecurityUser, String newEncodedPassword) {
        existingSecurityUser.setPassword(newEncodedPassword);
        existingSecurityUser.setUpdatedAt(LocalDateTime.now());
        return existingSecurityUser;
    }
}