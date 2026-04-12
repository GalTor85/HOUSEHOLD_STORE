package ru.galtor85.household_store.repository.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.payment.PaymentMethod;
import ru.galtor85.household_store.entity.payment.PaymentMethodType;
import ru.galtor85.household_store.entity.payment.PaymentProvider;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PaymentMethod entity
 */
@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    // =========================================================================
    // BASIC QUERIES
    // =========================================================================

    /**
     * Finds payment method by ID with eager loading
     *
     * @param id payment method ID
     * @return optional payment method
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.id = :id")
    Optional<PaymentMethod> findByIdWithDetails(@Param("id") Long id);

    /**
     * Finds all active payment methods
     *
     * @return list of active payment methods
     */
    List<PaymentMethod> findByActiveTrue();

    /**
     * Finds all inactive payment methods
     *
     * @return list of inactive payment methods
     */
    List<PaymentMethod> findByActiveFalse();

    /**
     * Finds default payment method
     *
     * @return optional default payment method
     */
    Optional<PaymentMethod> findByIsDefaultTrue();

    // =========================================================================
    // SEARCH BY TYPE
    // =========================================================================

    /**
     * Finds payment methods by type
     *
     * @param type payment method type
     * @return list of payment methods
     */
    List<PaymentMethod> findByMethodType(PaymentMethodType type);

    /**
     * Finds active payment methods by type
     *
     * @param type payment method type
     * @return list of active payment methods
     */
    List<PaymentMethod> findByMethodTypeAndActiveTrue(PaymentMethodType type);

    Optional<PaymentMethod> findByName(String name);

    // =========================================================================
    // SEARCH BY PROVIDER
    // =========================================================================

    /**
     * Finds payment methods by provider
     *
     * @param provider payment provider
     * @return list of payment methods
     */
    List<PaymentMethod> findByProvider(PaymentProvider provider);

    /**
     * Finds active payment methods by provider
     *
     * @param provider payment provider
     * @return list of active payment methods
     */
    List<PaymentMethod> findByProviderAndActiveTrue(PaymentProvider provider);

    // =========================================================================
    // USER SPECIFIC (if linked to user)
    // =========================================================================

    /**
     * Finds payment methods by user ID
     *
     * @param userId user ID
     * @return list of payment methods
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.createdBy = :userId")
    List<PaymentMethod> findByUserId(@Param("userId") Long userId);

    /**
     * Finds active payment methods by user ID
     *
     * @param userId user ID
     * @return list of active payment methods
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.createdBy = :userId AND pm.active = true")
    List<PaymentMethod> findActiveByUserId(@Param("userId") Long userId);

    /**
     * Finds default payment method for user
     *
     * @param userId user ID
     * @return optional default payment method
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.createdBy = :userId AND pm.isDefault = true")
    Optional<PaymentMethod> findDefaultByUserId(@Param("userId") Long userId);

    /**
     * Finds payment methods by user ID.
     */
    List<PaymentMethod> findByCreatedBy(Long userId);

    /**
     * Finds active payment methods by user ID.
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.createdBy = :userId AND pm.active = true")
    List<PaymentMethod> findActiveByCreatedBy(@Param("userId") Long userId);

    // =========================================================================
    // COUNT QUERIES
    // =========================================================================

    /**
     * Counts payment methods by type
     *
     * @param type payment method type
     * @return count
     */
    long countByMethodType(PaymentMethodType type);

    /**
     * Counts active payment methods by type
     *
     * @param type payment method type
     * @return count
     */
    long countByMethodTypeAndActiveTrue(PaymentMethodType type);

    /**
     * Counts payment methods by provider
     *
     * @param provider payment provider
     * @return count
     */
    long countByProvider(PaymentProvider provider);

    // =========================================================================
    // UPDATE QUERIES
    // =========================================================================

    /**
     * Deactivates all payment methods for a user
     *
     * @param userId user ID
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.active = false, pm.updatedAt = CURRENT_TIMESTAMP WHERE pm.createdBy = :userId")
    int deactivateAllByUserId(@Param("userId") Long userId);

    /**
     * Sets payment method as default (resets others for the same user)
     *
     * @param userId user ID
     * @param id payment method ID to set as default
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.isDefault = false WHERE pm.createdBy = :userId AND pm.id != :id")
    int resetDefaultForUser(@Param("userId") Long userId, @Param("id") Long id);

    /**
     * Activates payment method
     *
     * @param id payment method ID
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.active = true, pm.updatedAt = CURRENT_TIMESTAMP WHERE pm.id = :id")
    int activate(@Param("id") Long id);

    /**
     * Deactivates payment method
     *
     * @param id payment method ID
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.active = false, pm.updatedAt = CURRENT_TIMESTAMP WHERE pm.id = :id")
    int deactivate(@Param("id") Long id);

    // =========================================================================
    // EXISTENCE CHECKS
    // =========================================================================

    /**
     * Checks if user has any active payment methods
     *
     * @param userId user ID
     * @return true if has active methods
     */
    boolean existsByCreatedByAndActiveTrue(Long userId);

    /**
     * Checks if user has default payment method
     *
     * @param userId user ID
     * @return true if has default
     */
    boolean existsByCreatedByAndIsDefaultTrue(Long userId);
}