package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.security.SecurityUserFactory;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatusChangeProcessor {

    private final SecurityUserRepository securityUserRepository;
    private final SecurityUserFactory securityUserFactory;
    private final MessageService messageService;

    @Transactional
    public SecurityUser changeStatus(User targetUser, SecurityUser targetSecurity,
                                     boolean active, User adminUser) {

        boolean oldStatus = targetSecurity.isEnabled();

        SecurityUser updatedSecurityUser = securityUserFactory.withUpdatedStatus(
                targetSecurity,
                active
        );

        SecurityUser saved = securityUserRepository.save(updatedSecurityUser);

        String statusText = active ?
                messageService.get("user-status-service.user.status.active") :
                messageService.get("user-status-service.user.status.inactive");

        String oldStatusText = oldStatus ?
                messageService.get("user-status-service.user.status.active") :
                messageService.get("user-status-service.user.status.inactive");

        log.info(messageService.get(
                "user-status-service.log.status.changed",
                adminUser.getEmail(),
                targetUser.getEmail(),
                oldStatusText,
                statusText
        ));

        return saved;
    }
}