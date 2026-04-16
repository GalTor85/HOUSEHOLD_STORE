package ru.galtor85.household_store.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.user.UserTypeAssignment;

import java.util.List;
import java.util.Optional;

/**
 * Repository for user type assignment operations.
 */
@Repository
public interface UserTypeAssignmentRepository extends JpaRepository<UserTypeAssignment, Long> {

    /**
     * Finds all type assignments for a user.
     *
     * @param userId user ID
     * @return list of user type assignments
     */
    List<UserTypeAssignment> findByUserId(Long userId);

    /**
     * Finds active type assignment for a user.
     *
     * @param userId user ID
     * @return optional active user type assignment
     */
    @Query("SELECT uta FROM UserTypeAssignment uta WHERE uta.userId = :userId AND uta.active = true")
    Optional<UserTypeAssignment> findActiveByUserId(@Param("userId") Long userId);
}