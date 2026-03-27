package ru.galtor85.household_store.validator.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.auth.UserNotFoundException;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSearchValidator {

    private final UserRepository userRepository;
    private final MessageService messageService;

    public User validateUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-search-service.log.user.not.found.id", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    public void validateUserExistsById(Long userId) {
        if (!userRepository.existsById(userId)) {
            log.error(messageService.get("user-search-service.log.user.not.found.id", userId));
            throw new UserNotFoundException(userId.toString());
        }
    }
}