package ru.galtor85.household_store.repository.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.order.OrderType;
import ru.galtor85.household_store.entity.payment.PaymentTransaction;

import java.util.List;

/**
 * Repository for PaymentTransaction entity
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    // =========================================================================
    // SEARCH BY INVOICE/ORDER
    // =========================================================================

    /**
     * Finds transactions by order ID and type
     *
     * @param orderId order ID
     * @param orderType order type (PURCHASE, SALES)
     * @return list of transactions
     */
    List<PaymentTransaction> findByOrderIdAndOrderType(Long orderId, OrderType orderType);

    /**
     * Check if a refund already exists for the original transaction
     *
     * @param originalTransactionId original transaction ID
     * @return true if refund exists
     */
    boolean existsByOriginalTransactionId(Long originalTransactionId);

    /**
     * Counts transactions created by a user
     *
     * @param userId user ID
     * @return total count of transactions
     */
    long countByCreatedBy(Long userId);
}