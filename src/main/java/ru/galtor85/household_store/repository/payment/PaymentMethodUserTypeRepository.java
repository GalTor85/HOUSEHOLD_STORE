package ru.galtor85.household_store.repository.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.payment.PaymentMethodUserType;
import ru.galtor85.household_store.entity.user.UserType;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PaymentMethodUserType entity.
 *
 * @author G@LTor85
 
 */
@Repository
public interface PaymentMethodUserTypeRepository extends JpaRepository<PaymentMethodUserType, Long> {

    /**
     * Finds all assignments for a specific payment method.
     *
     * @param paymentMethodId payment method ID
     * @return list of assignments
     */
    List<PaymentMethodUserType> findByPaymentMethodId(Long paymentMethodId);

    /**
     * Finds active payment method IDs for a specific user type, ordered by sort order.
     *
     * @param userType the user type
     * @return list of payment method IDs
     */
    @Query("SELECT pmut.paymentMethodId FROM PaymentMethodUserType pmut " +
            "WHERE pmut.userType = :userType AND pmut.active = true " +
            "ORDER BY pmut.sortOrder ASC, pmut.id ASC")
    List<Long> findActivePaymentMethodIdsByUserType(@Param("userType") UserType userType);

    /**
     * Finds assignment by payment method ID and user type.
     *
     * @param paymentMethodId payment method ID
     * @param userType the user type
     * @return optional assignment
     */
    Optional<PaymentMethodUserType> findByPaymentMethodIdAndUserType(Long paymentMethodId, UserType userType);

    /**
     * Deletes all assignments for a payment method.
     *
     * @param paymentMethodId payment method ID
     */
    @Modifying
    @Query("DELETE FROM PaymentMethodUserType pmut WHERE pmut.paymentMethodId = :paymentMethodId")
    void deleteByPaymentMethodId(@Param("paymentMethodId") Long paymentMethodId);

    /**
     * Checks if a payment method is assigned to a user type.
     *
     * @param paymentMethodId payment method ID
     * @param userType the user type
     * @return true if assigned
     */
    boolean existsByPaymentMethodIdAndUserType(Long paymentMethodId, UserType userType);
}