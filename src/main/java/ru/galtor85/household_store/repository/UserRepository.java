
package ru.galtor85.household_store.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMobileNumber(String mobileNumber);
    Optional<User> findByEmailOrMobileNumber(String email, String mobileNumber);
    boolean existsByEmail(String email);
    boolean existsByMobileNumber(String mobileNumber);
    List<User> findByEmailContainingOrMobileNumberContainingOrFirstNameContainingOrLastNameContaining(String trimmedEmail, String trimmedMobile, String trimmedFirstName, String trimmedLastName, Sort by);
}