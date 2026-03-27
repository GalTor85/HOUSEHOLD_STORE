package ru.galtor85.household_store.processor.delete;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class SoftDeleteProcessor {

    private final UserRepository userRepository;
    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;

    @Transactional
    public void softDeleteUser(User user, SecurityUser securityUser) {
        String anonymizedEmail = "deleted_" + user.getId() + "_" + user.getEmail();

        user.setEmail(anonymizedEmail);
        user.setMobileNumber(null);

        securityUser.setActive(false);

        userRepository.save(user);
        securityUserRepository.save(securityUser);

        log.info(messageService.get("user-deleted-service.log.user.soft.deleted",
                user.getId(), anonymizedEmail));
    }
}