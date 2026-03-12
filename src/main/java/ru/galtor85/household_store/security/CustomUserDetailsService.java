package ru.galtor85.household_store.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.UserRepository;
import ru.galtor85.household_store.service.MessageService;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final MessageService messageService;

    @Override
    public UserDetails loadUserByUsername(String value) throws UsernameNotFoundException {
        User user = userRepository.findByEmailOrMobileNumber(value, value)
                .orElseThrow(() -> {
                    String errorMessage = messageService.get(
                            "custom-user-details-service.security.user.not.found",
                            value
                    );
                    log.error(errorMessage);
                    return new UsernameNotFoundException(errorMessage);
                });

        if (!user.isActive()) {
            String errorMessage = messageService.get(
                    "custom-user-details-service.security.user.inactive",
                    value
            );
            log.error(errorMessage);
            throw new UsernameNotFoundException(errorMessage);
        }

        log.info(messageService.get(
                "custom-user-details-service.security.user.authenticated",
                user.getEmail(),
                user.getRole()
        ));

        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return new org.springframework.security.core.userdetails.User(
                value,
                user.getPassword(),
                user.isActive(),
                true,
                true,
                true,
                authorities
        );
    }
}