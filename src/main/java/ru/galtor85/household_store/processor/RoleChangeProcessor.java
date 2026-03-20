package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.security.SecurityUserFactory;
import ru.galtor85.household_store.service.MessageService;

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
                targetUser,
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