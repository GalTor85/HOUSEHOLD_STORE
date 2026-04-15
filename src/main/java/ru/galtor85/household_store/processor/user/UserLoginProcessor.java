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
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.user.UserSearchService;
import ru.galtor85.household_store.util.email.EmailMasker;

/**
 * Processor for user login authentication.
 *
 * <p>Handles the core authentication logic for user login attempts.
 * This processor validates credentials, checks account status,
 * and retrieves the authenticated user entity.</p>
 *
 * <p>Authentication flow:</p>
 * <ol>
 *   <li>Find security user by email or mobile number</li>
 *   <li>Validate password using password encoder</li>
 *   <li>Check if account is active/enabled</li>
 *   <li>Retrieve and return the full user entity</li>
 * </ol>
 *
 * <p><b>Security note:</b> For security reasons, the same error message
 * is returned for invalid credentials and non-existent users to prevent
 * user enumeration attacks.</p>
 *
 * @author G@LTor85
 * @see SecurityUserRepository
 * @see PasswordEncoder
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserLoginProcessor {

    private static final String INVALID_CREDENTIALS_KEY = "user-service.error.login.invalid.credentials";
    private static final String ACCOUNT_DEACTIVATED_KEY = "user-service.error.login.account.deactivated";

    private final SecurityUserRepository securityUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSearchService userSearchService;
    private final MessageService messageService;
    private final LogMessageService logMsg;
    private final EmailMasker emailMasker;

    /**
     * Authenticates a user with the provided credentials.
     *
     * <p>Performs a multistep validation process:</p>
     * <ol>
     *   <li>Searches for the security user by email or mobile number</li>
     *   <li>Validates the provided password against the stored hash</li>
     *   <li>Checks if the user account is active and enabled</li>
     *   <li>Retrieves the full user entity on successful authentication</li>
     * </ol>
     *
     * @param password   the raw password provided by the user
     * @param identifier the user identifier (email or mobile number)
     * @return the authenticated User entity
     * @throws UserAuthenticationError if credentials are invalid or user not found
     * @throws UserNotActiveException  if the user account is deactivated
     */
    @Transactional(readOnly = true)
    public User login(String password, String identifier) {
        log.debug(logMsg.get("user.login.processor.start", emailMasker.maskIdentifier(identifier)));

        SecurityUser securityUser = findSecurityUserByIdentifier(identifier);
        validatePassword(password, securityUser.getPassword(), identifier);
        validateAccountActive(securityUser, identifier);

        User user = userSearchService.getUserById(securityUser.getUserId());

        log.info(logMsg.get("user-service.log.login.success", user.getEmail(), user.getId()));

        return user;
    }

    /**
     * Finds a security user by email or mobile number.
     *
     * @param identifier the user identifier (email or mobile number)
     * @return the SecurityUser entity
     * @throws UserAuthenticationError if user not found
     */
    private SecurityUser findSecurityUserByIdentifier(String identifier) {
        return securityUserRepository
                .findByEmailOrMobileNumber(identifier)
                .orElseThrow(() -> {
                    log.warn(logMsg.get("user-service.log.login.failed.not.found", emailMasker.maskIdentifier(identifier)));
                    return new UserAuthenticationError(
                            messageService.get(INVALID_CREDENTIALS_KEY)
                    );
                });
    }

    /**
     * Validates the provided password against the stored hash.
     *
     * @param rawPassword     the raw password provided by the user
     * @param encodedPassword the stored encoded password hash
     * @param identifier      the user identifier (for logging context)
     * @throws UserAuthenticationError if password does not match
     */
    private void validatePassword(String rawPassword, String encodedPassword, String identifier) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            log.warn(logMsg.get("user-service.log.login.failed.wrong.password", emailMasker.maskIdentifier(identifier)));
            throw new UserAuthenticationError(
                    messageService.get(INVALID_CREDENTIALS_KEY)
            );
        }
    }

    /**
     * Validates that the user account is active and enabled.
     *
     * @param securityUser the security user to validate
     * @param identifier   the user identifier (for logging context)
     * @throws UserNotActiveException if the account is deactivated
     */
    private void validateAccountActive(SecurityUser securityUser, String identifier) {
        if (!securityUser.isEnabled()) {
            log.warn(logMsg.get("user-service.log.login.failed.deactivated", emailMasker.maskIdentifier(identifier)));
            throw new UserNotActiveException(
                    messageService.get(ACCOUNT_DEACTIVATED_KEY)
            );
        }
    }
}