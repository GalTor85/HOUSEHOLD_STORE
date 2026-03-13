package ru.galtor85.household_store.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;

    @Override
    public UserDetails loadUserByUsername(String value) throws UsernameNotFoundException {
        // ИСПРАВЛЕНО: используем новое имя метода
        SecurityUser securityUser = securityUserRepository
                .findByEmailOrMobileNumber(value)
                .orElseThrow(() -> {
                    String errorMessage = messageService.get(
                            "custom-user-details-service.security.user.not.found",
                            value
                    );
                    log.error(errorMessage);
                    return new UsernameNotFoundException(errorMessage);
                });

        if (!securityUser.isEnabled()) {
            String errorMessage = messageService.get(
                    "custom-user-details-service.security.user.inactive",
                    value
            );
            log.error(errorMessage);
            throw new UsernameNotFoundException(errorMessage);
        }

        log.info(messageService.get(
                "custom-user-details-service.security.user.authenticated",
                value,
                securityUser.getRole()
        ));

        return securityUser;
    }
}