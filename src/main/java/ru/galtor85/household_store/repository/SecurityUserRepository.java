package ru.galtor85.household_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.security.SecurityUser;

import java.util.Optional;

@Repository
public interface SecurityUserRepository extends JpaRepository<SecurityUser, Long> {
    @Query("SELECT su FROM SecurityUser su WHERE su.userId IN " +
            "(SELECT u.id FROM User u WHERE u.email = :value OR u.mobileNumber = :value)")
    Optional<SecurityUser> findByEmailOrMobileNumber(@Param("value") String value);

    Optional<SecurityUser> findByUserId(Long userId);

    void deleteByUserId(Long finalUserId);

    boolean existsByUserId(Long userId);
}