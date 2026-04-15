package ru.galtor85.household_store.processor.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.auth.UserNotFoundException;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.util.Optional;

/**
 * Processor for security user operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityUserProcessor {

    private final SecurityUserRepository securityUserRepository;
    private final LogMessageService logMsg;

    /**
     * Gets a security user by user ID.
     *
     * @param userId the user ID
     * @return SecurityUser entity
     * @throws UserNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public SecurityUser getSecurityUserByUserId(Long userId) {
        return securityUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("user-search-service.log.security.user.not.found", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    /**
     * Finds a security user by user ID.
     *
     * @param userId the user ID
     * @return Optional containing SecurityUser if found
     */
    @Transactional(readOnly = true)
    public Optional<SecurityUser> findSecurityUserByUserId(Long userId) {
        return securityUserRepository.findById(userId);
    }
}