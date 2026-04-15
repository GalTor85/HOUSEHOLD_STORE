package ru.galtor85.household_store.processor.delete;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import static ru.galtor85.household_store.constants.TechnicalConstants.EMAIL_ANONYMIZED_PREFIX;

/**
 * Processor for soft deletion of users.
 * Anonymizes user data and deactivates account instead of physical deletion.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SoftDeleteProcessor {

    private final UserRepository userRepository;
    private final SecurityUserRepository securityUserRepository;
    private final LogMessageService logMsg;
    private final MessageService messageService;


    /**
     * Performs soft delete of a user.
     * Anonymizes email, clears mobile number, and deactivates the security account.
     *
     * @param user   the user to softly delete
     * @param userId the user ID
     * @throws IllegalArgumentException if security user not found
     */
    @Transactional
    public void softDeleteUser(User user, Long userId) {
        String anonymizedEmail = EMAIL_ANONYMIZED_PREFIX
                + user.getId()
                + "_"
                + user.getEmail();

        user.setEmail(anonymizedEmail);
        user.setMobileNumber(null);

        SecurityUser securityUser = securityUserRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("user.deleted.security.not.found", userId)));
        securityUser.setActive(false);

        userRepository.save(user);
        securityUserRepository.save(securityUser);

        log.info(logMsg.get("user-deleted-service.log.user.soft.deleted",
                user.getId(), anonymizedEmail));
    }
}