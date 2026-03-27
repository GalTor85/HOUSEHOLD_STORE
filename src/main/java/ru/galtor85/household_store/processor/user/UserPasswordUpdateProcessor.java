package ru.galtor85.household_store.processor.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.request.user.UserUpdatePasswordRequest;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.security.SecurityUserFactory;
import ru.galtor85.household_store.service.i18n.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPasswordUpdateProcessor {

    private final SecurityUserRepository securityUserRepository;
    private final SecurityUserFactory securityUserFactory;
    private final PasswordEncoder passwordEncoder;
    private final MessageService messageService;

    @Transactional
    public void updatePassword(User user, SecurityUser existingSecurityUser,
                               UserUpdatePasswordRequest request) {

        SecurityUser updatedSecurityUser = securityUserFactory.withUpdatedPassword(
                existingSecurityUser,
                passwordEncoder.encode(request.getNewPassword())
        );

        securityUserRepository.save(updatedSecurityUser);

        log.info(messageService.get("user-service.log.password.updated",
                maskEmail(user.getEmail())));
    }

    private String maskEmail(String email) {
        if (email == null) return "null";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email;

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        return localPart.charAt(0) + "***" +
                (localPart.length() > 2 ? localPart.charAt(localPart.length() - 1) : "") +
                domain;
    }
}