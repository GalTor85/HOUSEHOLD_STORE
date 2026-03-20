package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.UserNotFoundException;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.MessageService;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityUserProcessor {

    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public SecurityUser getSecurityUserByUserId(Long userId) {
        return securityUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-search-service.log.security.user.not.found", userId));
                    return new UserNotFoundException(userId.toString());
                });
    }

    @Transactional(readOnly = true)
    public Optional<SecurityUser> findSecurityUserByUserId(Long userId) {
        return securityUserRepository.findById(userId);
    }
}