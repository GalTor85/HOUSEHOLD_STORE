package ru.galtor85.household_store.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.response.user.UserStatistics;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.processor.user.UserSearchProcessor;
import ru.galtor85.household_store.processor.user.UserStatisticsProcessor;
import ru.galtor85.household_store.validator.auth.UserSearchValidator;
import ru.galtor85.household_store.validator.common.SortFieldValidator;

import java.util.List;

/**
 * Service for searching users and retrieving user statistics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSearchService {

    private final UserSearchValidator validator;
    private final UserSearchProcessor searchProcessor;
    private final UserStatisticsProcessor statisticsProcessor;
    private final SortFieldValidator sortFieldValidator;

    /**
     * Gets all users with sorting.
     *
     * @param sort sort field
     * @return list of users
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers(String sort) {
        String validSort = sortFieldValidator.validateAndGetSortField(sort);
        return searchProcessor.getAllUsers(validSort);
    }

    /**
     * Searches users by criteria.
     *
     * @param mobileNumber mobile number filter
     * @param email email filter
     * @param firstName first name filter
     * @param lastName last name filter
     * @param sort sort field
     * @return list of matching users
     */
    @Transactional(readOnly = true)
    public List<User> searchUsersByCriteria(String mobileNumber, String email,
                                            String firstName, String lastName,
                                            String sort) {
        String validSort = sortFieldValidator.validateAndGetSortField(sort);
        return searchProcessor.searchUsersByCriteria(
                mobileNumber, email, firstName, lastName, validSort);
    }

    /**
     * Gets user by ID.
     *
     * @param userId user ID
     * @return user entity
     */
    public User getUserById(Long userId) {
        return validator.validateUserExists(userId);
    }

    /**
     * Gets user statistics.
     *
     * @return user statistics DTO
     */
    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics() {
        return statisticsProcessor.calculateStatistics();
    }
}
