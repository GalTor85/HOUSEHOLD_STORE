package ru.galtor85.household_store.validator.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.auth.UserNotFoundException;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Validator for user search operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSearchValidator {

    private final UserRepository userRepository;
    private final SecurityUserRepository securityUserRepository;
    private final LogMessageService logMsg;
    private final MessageService messageService;

    /**
     * Validates user exists by ID.
     *
     * @param userId user ID
     * @return user entity
     * @throws UserNotFoundException if user or security user not found
     */
    public User validateUserExists(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("user-search-service.log.user.not.found.id", userId));
                    return new UserNotFoundException(userId.toString());
                });

        validateSecurityUserExists(userId);

        return user;
    }

    /**
     * Validates security user exists for the given user ID.
     *
     * @param userId user ID
     * @throws UserNotFoundException if security user not found
     */
    public void validateSecurityUserExists(Long userId) {
        if (!securityUserRepository.existsByUserId(userId)) {
            log.error(logMsg.get("user-search-service.log.security.user.not.found", userId));
            throw new UserNotFoundException(
                    messageService.get("user-search-service.error.security.user.missing", userId)
            );
        }
    }
}