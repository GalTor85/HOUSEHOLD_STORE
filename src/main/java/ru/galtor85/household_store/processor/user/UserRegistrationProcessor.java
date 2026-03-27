package ru.galtor85.household_store.processor.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.security.SecurityUserFactory;
import ru.galtor85.household_store.service.i18n.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegistrationProcessor {

    private final UserRepository userRepository;
    private final SecurityUserRepository securityUserRepository;
    private final SecurityUserFactory securityUserFactory;
    private final PasswordEncoder passwordEncoder;
    private final MessageService messageService;

    @Transactional
    public User register(User user, String rawPassword, Role role) {
        User savedUser = userRepository.save(user);

        SecurityUser securityUser = securityUserFactory.createNew(
                savedUser,
                passwordEncoder.encode(rawPassword),
                role != null ? role : Role.USER
        );

        securityUserRepository.save(securityUser);

        log.info(messageService.get("user-service.log.user.newregistered",
                savedUser.getEmail(), savedUser.getId()));

        return savedUser;
    }
}