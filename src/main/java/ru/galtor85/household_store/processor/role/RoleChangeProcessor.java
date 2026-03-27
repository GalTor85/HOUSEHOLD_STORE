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
import ru.galtor85.household_store.service.i18n.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleChangeProcessor {

    private final SecurityUserRepository securityUserRepository;
    private final SecurityUserFactory securityUserFactory;
    private final MessageService messageService;

    @Transactional
    public SecurityUser changeRole(User targetUser, SecurityUser targetSecurity,
                                   Role newRole, User adminUser) {

        Role oldRole = targetSecurity.getRole();

        SecurityUser updatedSecurityUser = securityUserFactory.withUpdatedRole(
                targetSecurity,
                newRole
        );

        SecurityUser saved = securityUserRepository.save(updatedSecurityUser);

        log.info(messageService.get(
                "user-role-service.log.role.changed",
                adminUser.getEmail(),
                targetUser.getEmail(),
                oldRole,
                newRole
        ));

        return saved;
    }
}