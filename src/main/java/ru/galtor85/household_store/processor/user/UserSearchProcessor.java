package ru.galtor85.household_store.processor.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.util.List;
import java.util.Optional;

import static ru.galtor85.household_store.constants.PaginationConstants.DEFAULT_SORT_FIELD;

/**
 * Processor for user search operations.
 *
 * <p>Handles all user lookup operations including:
 * <ul>
 *   <li>Retrieving all users with sorting</li>
 *   <li>Searching users by multiple criteria</li>
 *   <li>Finding users by email or mobile number</li>
 *   <li>Checking user existence</li>
 * </ul>
 *
 * <p>All methods in this processor are read-only and execute within
 * a transaction for consistency.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSearchProcessor {

    private final UserRepository userRepository;
    private final LogMessageService logMsg;

    /**
     * Retrieves all users with optional sorting.
     *
     * @param sort the field to sort by (uses "id" if null or empty)
     * @return list of all users sorted by the specified field
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers(String sort) {
        String sortField = normalizeSortField(sort);
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.ASC, sortField));

        log.debug(logMsg.get(
                "user-search-service.log.user.search.all",
                users.size(),
                sortField
        ));

        return users;
    }

    /**
     * Searches users by multiple optional criteria.
     *
     * <p>All criteria are optional - if a criterion is null, it is ignored.
     * The search uses partial matching (contains) for all text fields.</p>
     *
     * @param mobileNumber optional mobile number filter
     * @param email optional email filter
     * @param firstName optional first name filter
     * @param lastName optional last name filter
     * @param sort the field to sort by (uses "id" if null or empty)
     * @return list of users matching the specified criteria
     */
    @Transactional(readOnly = true)
    public List<User> searchUsersByCriteria(String mobileNumber,
                                            String email,
                                            String firstName,
                                            String lastName,
                                            String sort) {
        String trimmedEmail = trimToNull(email);
        String trimmedMobile = trimToNull(mobileNumber);
        String trimmedFirstName = trimToNull(firstName);
        String trimmedLastName = trimToNull(lastName);
        String sortField = normalizeSortField(sort);

        List<User> users = userRepository.findByEmailContainingOrMobileNumberContainingOrFirstNameContainingOrLastNameContaining(
                trimmedEmail, trimmedMobile, trimmedFirstName, trimmedLastName,
                Sort.by(Sort.Direction.ASC, sortField));

        log.debug(logMsg.get(
                "user-search-service.log.user.search.criteria",
                users.size(),
                trimmedEmail,
                trimmedMobile,
                trimmedFirstName,
                trimmedLastName
        ));

        return users;
    }

    /**
     * Searches for a user by email or mobile number.
     *
     * <p>Performs exact match search using the provided identifier
     * against both email and mobile number fields.</p>
     *
     * @param identifier the email or mobile number to search for
     * @return Optional containing the user if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<User> searchUsersByEmailOrMobileNumber(String identifier) {
        Optional<User> user = userRepository.findByEmailOrMobileNumber(identifier, identifier);

        if (user.isPresent()) {
            log.debug(logMsg.get(
                    "user-search-service.log.user.search.identify.found",
                    identifier,
                    user.get().getEmail()
            ));
        } else {
            log.debug(logMsg.get(
                    "user-search-service.log.user.search.identify.not.found",
                    identifier
            ));
        }

        return user;
    }

    /**
     * Finds a user by exact email match.
     *
     * @param email the email address to search for
     * @return Optional containing the user if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);

        log.debug(logMsg.get(
                user.isPresent()
                        ? "user-search-service.log.user.find.email.found"
                        : "user-search-service.log.user.find.email.not.found",
                email
        ));

        return user;
    }

    /**
     * Checks if a user exists with the given email address.
     *
     * @param email the email address to check
     * @return true if a user with this email exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean userExistsByEmail(String email) {
        boolean exists = userRepository.existsByEmail(email);

        log.debug(logMsg.get(
                "user-search-service.log.user.exists.email",
                email,
                exists
        ));

        return exists;
    }

    /**
     * Checks if a user exists with the given ID.
     *
     * @param userId the user ID to check
     * @return true if a user with this ID exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean userExistsById(Long userId) {
        boolean exists = userRepository.existsById(userId);

        log.debug(logMsg.get(
                "user-search-service.log.user.exists.id",
                userId,
                exists
        ));

        return exists;
    }

    /**
     * Normalizes the sort field string.
     *
     * @param sort the raw sort field
     * @return trimmed sort field or default "id" if null/empty
     */
    private String normalizeSortField(String sort) {
        return sort != null && !sort.isBlank() ? sort.trim() : DEFAULT_SORT_FIELD;
    }

    /**
     * Trims a string to null if empty.
     *
     * @param value the string to trim
     * @return trimmed string or null if empty
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}