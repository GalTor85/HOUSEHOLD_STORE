package ru.galtor85.household_store.security;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.email.EmailMasker;

/**
 * Custom implementation of Spring Security's UserDetailsService.
 *
 * <p>Loads user-specific data during authentication. Supports authentication
 * by either email or mobile number as the username.</p>
 *
 * <p>Security features:
 * <ul>
 *   <li>Masquerades the reason for authentication failure to prevent user enumeration</li>
 *   <li>Masks sensitive identifiers in logs for privacy</li>
 *   <li>Checks account status before allowing authentication</li>
 * </ul>
 *
 * @author G@LTor85
 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;
    private final EmailMasker emailMasker;

    /**
     * Locates the user based on the username (email or mobile number).
     *
     * <p>Performs the following checks:
     * <ol>
     *   <li>Finds the security user by email or mobile number</li>
     *   <li>Verifies that the account is enabled</li>
     *   <li>Returns the UserDetails object for Spring Security</li>
     * </ol>
     *
     * <p><b>Security note:</b> To prevent user enumeration attacks, the same
     * error message is used for both non-existent users and inactive accounts
     * during the initial lookup phase.</p>
     *
     * @param username the username identifying the user (email or mobile number)
     * @return a fully populated UserDetails object
     * @throws UsernameNotFoundException if the user could not be found or is inactive
     */

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        String maskedUsername = maskUsername(username);
        log.debug(logMsg.get("custom.user.details.service.loading", maskedUsername));

        SecurityUser securityUser = findSecurityUser(username);
        validateAccountActive(securityUser, username);

        log.info(logMsg.get(
                "custom-user-details-service.security.user.authenticated",
                maskedUsername,
                securityUser.getRole()
        ));

        return securityUser;
    }

    /**
     * Finds a security user by email or mobile number.
     *
     * @param username the email or mobile number to search for
     * @return the SecurityUser entity
     * @throws UsernameNotFoundException if user not found
     */
    private SecurityUser findSecurityUser(String username) {
        return securityUserRepository
                .findByEmailOrMobileNumber(username)
                .orElseThrow(() -> {
                    String errorMessage = messageService.get(
                            "custom-user-details-service.security.user.not.found",
                            maskUsername(username)
                    );
                    log.warn(errorMessage);
                    return new UsernameNotFoundException(errorMessage);
                });
    }

    /**
     * Validates that the user account is active and enabled.
     *
     * @param securityUser the security user to validate
     * @param username the original username for logging context
     * @throws UsernameNotFoundException if account is inactive
     */
    private void validateAccountActive(SecurityUser securityUser, String username) {
        if (!securityUser.isEnabled()) {
            String errorMessage = messageService.get(
                    "custom-user-details-service.security.user.inactive",
                    maskUsername(username)
            );
            log.warn(errorMessage);
            throw new UsernameNotFoundException(errorMessage);
        }
    }

    /**
     * Masks the username for secure logging.
     *
     * <p>For email addresses, masks the local part.
     * For phone numbers, shows only the last 4 digits.</p>
     *
     * @param username the username to mask
     * @return masked username safe for logging
     */
    private String maskUsername(String username) {
        if (username == null) {
            return null;
        }

        if (username.contains("@")) {
            return emailMasker.maskEmail(username);
        }

        return emailMasker.maskPhoneNumber(username);
    }
}