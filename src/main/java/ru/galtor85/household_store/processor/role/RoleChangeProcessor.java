package ru.galtor85.household_store.processor.role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.security.SecurityUserFactory;
import ru.galtor85.household_store.service.i18n.LogMessageService;

/**
 * Processor for changing user roles.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleChangeProcessor {

    private final SecurityUserRepository securityUserRepository;
    private final SecurityUserFactory securityUserFactory;
    private final LogMessageService logMsg;

    /**
     * Changes the role of a user.
     *
     * @param targetUser     the target user entity
     * @param targetSecurity the target security user
     * @param newRole        the new role
     * @param adminUser      the admin performing the change
     */
    @Transactional
    public void changeRole(User targetUser, SecurityUser targetSecurity,
                           Role newRole, User adminUser) {

        Role oldRole = targetSecurity.getRole();

        SecurityUser updatedSecurityUser = securityUserFactory.withUpdatedRole(
                targetSecurity,
                newRole
        );

        securityUserRepository.save(updatedSecurityUser);

        log.info(logMsg.get(
                "user-role-service.log.role.changed",
                adminUser.getEmail(),
                targetUser.getEmail(),
                oldRole,
                newRole
        ));

    }
}