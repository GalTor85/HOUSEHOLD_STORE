package ru.galtor85.household_store.processor.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.auth.UserAuthenticationError;
import ru.galtor85.household_store.advice.exception.auth.UserNotActiveException;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.user.UserSearchService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserLoginProcessor {

    private final SecurityUserRepository securityUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSearchService userSearchService;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public User login(String password, String value) {
        SecurityUser securityUser = securityUserRepository
                .findByEmailOrMobileNumber(value)
                .orElseThrow(() -> {
                    log.warn(messageService.get("user-service.log.login.failed.not.found", value));
                    return new UserAuthenticationError(
                            messageService.get("user-service.error.login.invalid.credentials")
                    );
                });

        if (!passwordEncoder.matches(password, securityUser.getPassword())) {
            log.warn(messageService.get("user-service.log.login.failed.wrong.password", value));
            throw new UserAuthenticationError(
                    messageService.get("user-service.error.login.invalid.credentials")
            );
        }

        if (!securityUser.isEnabled()) {
            log.warn(messageService.get("user-service.log.login.failed.deactivated", value));
            throw new UserNotActiveException(
                    messageService.get("user-service.error.login.account.deactivated")
            );
        }

        User user = userSearchService.getUserById(securityUser.getUserId());
        log.info(messageService.get("user-service.log.login.success", user.getEmail(), user.getId()));

        return user;
    }
}