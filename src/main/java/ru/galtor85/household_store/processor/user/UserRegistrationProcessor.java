package ru.galtor85.household_store.processor.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.security.SecurityUserFactory;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.user.UserTypeAssignmentService;
import ru.galtor85.household_store.util.email.EmailMasker;

import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_REASON_FOR_CREATE;
import static ru.galtor85.household_store.constants.TechnicalConstants.SYSTEM_CREATOR;

/**
 * Processor for user registration operations.
 *
 * <p>Handles creation of User entity, SecurityUser entity for authentication,
 * and automatic assignment of default user type for self-registered customers.</p>
 *
 * <p>Registration flow:
 * <ol>
 *   <li>Save the User entity to the database</li>
 *   <li>Create SecurityUser with encoded password for authentication</li>
 *   <li>Assign default RETAIL user type for self-registered customers</li>
 *   <li>Save the SecurityUser entity</li>
 * </ol>
 *
 * @author G@LTor85
 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegistrationProcessor {

    private final UserRepository userRepository;
    private final SecurityUserRepository securityUserRepository;
    private final SecurityUserFactory securityUserFactory;
    private final PasswordEncoder passwordEncoder;
    private final LogMessageService logMsg;
    private final UserTypeAssignmentService userTypeAssignmentService;
    private final EmailMasker emailMasker;

    /**
     * Registers a new user in the system.
     *
     * <p>Creates both the domain User entity and the SecurityUser entity
     * required for authentication. For self-registered users (those with
     * null creator and USER role), automatically assigns the RETAIL user type.</p>
     *
     * @param user the user entity to register (must have email/mobile and personal info)
     * @param rawPassword the raw (unencoded) password provided by the user
     * @param role the security role to assign (defaults to USER if null)
     * @return the saved User entity with generated ID
     */
    @Transactional
    public User register(User user, String rawPassword, Role role) {
        String maskedEmail = emailMasker.maskEmail(user.getEmail());
        log.debug(logMsg.get("user.registration.start", maskedEmail));

        User savedUser = userRepository.save(user);

        Role effectiveRole = role != null ? role : Role.USER;
        String encodedPassword = passwordEncoder.encode(rawPassword);

        SecurityUser securityUser = securityUserFactory.createNew(
                savedUser,
                encodedPassword,
                effectiveRole
        );

        assignDefaultUserTypeIfSelfRegistered(savedUser, effectiveRole);
        securityUserRepository.save(securityUser);

        log.info(logMsg.get("user.registration.success", maskedEmail, savedUser.getId()));

        return savedUser;
    }

    /**
     * Assigns the default RETAIL user type for self-registered customers.
     *
     * <p>A user is considered self-registered if:
     * <ul>
     *   <li>The role is USER (not ADMIN or MANAGER)</li>
     *   <li>The creator field is null (not created by an admin)</li>
     * </ul>
     *
     * @param user the saved user entity
     * @param role the assigned security role
     */
    private void assignDefaultUserTypeIfSelfRegistered(User user, Role role) {
        boolean isSelfRegistered = role == Role.USER && user.getCreator() == null;

        if (isSelfRegistered) {
            user.setCreator(SYSTEM_CREATOR);
            userTypeAssignmentService.assignUserType(
                    user.getId(),
                    UserType.RETAIL,
                    SYSTEM_CREATOR,
                    DEFAULT_REASON_FOR_CREATE
            );
            log.debug(logMsg.get("user.registration.default.type.assigned",
                    emailMasker.maskEmail(user.getEmail())));
        }
    }
}