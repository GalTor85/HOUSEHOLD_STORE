package ru.galtor85.household_store.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.entity.user.UserTypeAssignment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTypeAssignmentRepository extends JpaRepository<UserTypeAssignment, Long> {

    Optional<UserTypeAssignment> findByUserIdAndActiveTrue(Long userId);

    List<UserTypeAssignment> findByUserId(Long userId);

    List<UserTypeAssignment> findByUserType(UserType userType);

    List<UserTypeAssignment> findByAssignedBy(String assignedBy);
    /**
     * Найти все активные назначения для указанного типа пользователя
     */
    @Query("SELECT uta FROM UserTypeAssignment uta " +
            "WHERE uta.userType = :userType AND uta.active = true")
    List<UserTypeAssignment> findActiveByUserType(@Param("userType") UserType userType);

    @Query("SELECT uta FROM UserTypeAssignment uta WHERE uta.userId = :userId AND uta.active = true")
    Optional<UserTypeAssignment> findActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT uta FROM UserTypeAssignment uta WHERE uta.userId = :userId AND uta.validFrom <= :now AND (uta.validTo IS NULL OR uta.validTo >= :now)")
    List<UserTypeAssignment> findValidByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    boolean existsByUserIdAndUserType(Long userId, UserType userType);

    @Query("SELECT COUNT(uta) FROM UserTypeAssignment uta WHERE uta.userType = :userType AND uta.active = true")
    long countByUserType(@Param("userType") UserType userType);
}