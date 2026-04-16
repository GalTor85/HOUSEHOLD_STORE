package ru.galtor85.household_store.repository.user;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.user.User;

import java.util.List;
import java.util.Optional;

/**
 * Repository for user operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds user by email.
     *
     * @param email user email
     * @return optional user
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if user exists by email.
     *
     * @param email user email
     * @return true if exists
     */
    boolean existsByEmail(String email);

    /**
     * Checks if user exists by mobile number.
     *
     * @param mobileNumber user mobile number
     * @return true if exists
     */
    boolean existsByMobileNumber(String mobileNumber);

    /**
     * Searches users by multiple criteria with partial matching.
     *
     * @param email email filter
     * @param mobile mobile number filter
     * @param firstName first name filter
     * @param lastName last name filter
     * @param sort sort configuration
     * @return list of matching users
     */
    List<User> findByEmailContainingOrMobileNumberContainingOrFirstNameContainingOrLastNameContaining(
            String email, String mobile, String firstName, String lastName, Sort sort);
}