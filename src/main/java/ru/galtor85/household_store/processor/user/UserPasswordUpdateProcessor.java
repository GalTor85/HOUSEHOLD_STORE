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
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.util.email.EmailMasker;

/**
 * Processor for updating user passwords.
 *
 * <p>Handles the secure password change operation for authenticated users.
 * This processor ensures that password updates are properly encoded and
 * persisted to the security storage.</p>
 *
 * <p><b>Security features:</b>
 * <ul>
 *   <li>Passwords are encoded using BCrypt before storage</li>
 *   <li>Email addresses are masked in logs for privacy</li>
 *   <li>Updates are performed within a transaction</li>
 * </ul>
 *
 * @author G@LTor85
 * @since 1.0
 * @see SecurityUserFactory
 * @see PasswordEncoder
 * @see EmailMasker
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserPasswordUpdateProcessor {

    private final SecurityUserRepository securityUserRepository;
    private final SecurityUserFactory securityUserFactory;
    private final PasswordEncoder passwordEncoder;
    private final LogMessageService logMsg;
    private final EmailMasker emailMasker;

    /**
     * Updates the password for a user.
     *
     * <p>Performs the following steps:
     * <ol>
     *   <li>Encodes the new password using the configured password encoder</li>
     *   <li>Creates an updated SecurityUser entity with the new password</li>
     *   <li>Persists the updated security user to the database</li>
     *   <li>Logs the successful password change with masked email</li>
     * </ol>
     *
     * <p><b>Note:</b> This method assumes that all necessary validations
     * (current password verification, password complexity, confirmation match)
     * have already been performed by the calling service.</p>
     *
     * @param user the domain user entity (used for logging context)
     * @param existingSecurityUser the current security user entity with old password
     * @param request the password update request containing the new password
     */
    @Transactional
    public void updatePassword(User user,
                               SecurityUser existingSecurityUser,
                               UserUpdatePasswordRequest request) {

        String maskedEmail = emailMasker.maskEmail(user.getEmail());

        log.debug(logMsg.get("user.password.update.start", maskedEmail));

        SecurityUser updatedSecurityUser = securityUserFactory.withUpdatedPassword(
                existingSecurityUser,
                passwordEncoder.encode(request.getNewPassword())
        );

        securityUserRepository.save(updatedSecurityUser);

        log.info(logMsg.get("user.password.updated", maskedEmail));
    }
}