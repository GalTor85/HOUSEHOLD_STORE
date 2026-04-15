package ru.galtor85.household_store.repository.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.order.OrderType;
import ru.galtor85.household_store.entity.payment.PaymentTransaction;
import ru.galtor85.household_store.entity.payment.PaymentTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PaymentTransaction entity
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    // =========================================================================
    // BASIC QUERIES
    // =========================================================================

    /**
     * Finds payment transaction by provider transaction ID
     *
     * @param providerTransactionId provider's transaction ID
     * @return optional payment transaction
     */
    Optional<PaymentTransaction> findByProviderTransactionId(String providerTransactionId);

    /**
     * Finds all transactions for a specific payment method
     *
     * @param paymentMethodId payment method ID
     * @return list of transactions
     */
    List<PaymentTransaction> findByPaymentMethodId(Long paymentMethodId);

    /**
     * Finds paginated transactions for a payment method
     *
     * @param paymentMethodId payment method ID
     * @param pageable pagination info
     * @return page of transactions
     */
    Page<PaymentTransaction> findByPaymentMethodId(Long paymentMethodId, Pageable pageable);

    // =========================================================================
    // SEARCH BY INVOICE/ORDER
    // =========================================================================

    /**
     * Finds transactions by invoice ID
     *
     * @param invoiceId invoice ID
     * @return list of transactions
     */
    List<PaymentTransaction> findByInvoiceId(Long invoiceId);

    /**
     * Finds transactions by order ID and type
     *
     * @param orderId order ID
     * @param orderType order type (PURCHASE, SALES)
     * @return list of transactions
     */
    List<PaymentTransaction> findByOrderIdAndOrderType(Long orderId, OrderType orderType);

    /**
     * Finds successful transactions for an invoice
     *
     * @param invoiceId invoice ID
     * @return list of completed transactions
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.invoiceId = :invoiceId AND pt.status = 'COMPLETED'")
    List<PaymentTransaction> findSuccessfulByInvoiceId(@Param("invoiceId") Long invoiceId);

    // =========================================================================
    // SEARCH BY STATUS
    // =========================================================================

    /**
     * Finds transactions by status
     *
     * @param status transaction status
     * @return list of transactions
     */
    List<PaymentTransaction> findByStatus(PaymentTransactionStatus status);

    /**
     * Finds paginated transactions by status
     *
     * @param status transaction status
     * @param pageable pagination info
     * @return page of transactions
     */
    Page<PaymentTransaction> findByStatus(PaymentTransactionStatus status, Pageable pageable);

    /**
     * Finds pending transactions older than specified date
     *
     * @param date cutoff date
     * @return list of stale pending transactions
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.status = 'PENDING' AND pt.createdAt < :date")
    List<PaymentTransaction> findStalePendingTransactions(@Param("date") LocalDateTime date);

    // =========================================================================
    // SEARCH BY DATE RANGE
    // =========================================================================

    /**
     * Finds transactions within date range
     *
     * @param startDate start date
     * @param endDate end date
     * @return list of transactions
     */
    List<PaymentTransaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Finds completed transactions within date range
     *
     * @param startDate start date
     * @param endDate end date
     * @return list of completed transactions
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.status = 'COMPLETED' AND pt.completedAt BETWEEN :startDate AND :endDate")
    List<PaymentTransaction> findCompletedBetween(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    // =========================================================================
    // AGGREGATION QUERIES
    // =========================================================================

    /**
     * Gets total amount of completed transactions for an invoice
     *
     * @param invoiceId invoice ID
     * @return total paid amount
     */
    @Query("SELECT COALESCE(SUM(pt.amount), 0) FROM PaymentTransaction pt WHERE pt.invoiceId = :invoiceId AND pt.status = 'COMPLETED'")
    BigDecimal getTotalPaidForInvoice(@Param("invoiceId") Long invoiceId);

    /**
     * Gets total amount of completed transactions for an order
     *
     * @param orderId order ID
     * @param orderType order type
     * @return total paid amount
     */
    @Query("SELECT COALESCE(SUM(pt.amount), 0) FROM PaymentTransaction pt WHERE pt.orderId = :orderId AND pt.orderType = :orderType AND pt.status = 'COMPLETED'")
    BigDecimal getTotalPaidForOrder(@Param("orderId") Long orderId,
                                    @Param("orderType") String orderType);

    /**
     * Gets daily transaction summary
     *
     * @param date date
     * @return summary [count, totalAmount]
     */
    @Query("SELECT COUNT(pt), COALESCE(SUM(pt.amount), 0) FROM PaymentTransaction pt WHERE DATE(pt.createdAt) = DATE(:date) AND pt.status = 'COMPLETED'")
    List<Object[]> getDailySummary(@Param("date") LocalDateTime date);

    /**
     * Gets monthly transaction summary
     *
     * @param year year
     * @param month month
     * @return summary [count, totalAmount]
     */
    @Query("SELECT COUNT(pt), COALESCE(SUM(pt.amount), 0) FROM PaymentTransaction pt WHERE YEAR(pt.createdAt) = :year AND MONTH(pt.createdAt) = :month AND pt.status = 'COMPLETED'")
    List<Object[]> getMonthlySummary(@Param("year") int year, @Param("month") int month);

    /**
     * Gets transaction statistics by payment method
     *
     * @return list of [paymentMethodId, count, totalAmount]
     */
    @Query("SELECT pt.paymentMethodId, COUNT(pt), COALESCE(SUM(pt.amount), 0) FROM PaymentTransaction pt WHERE pt.status = 'COMPLETED' GROUP BY pt.paymentMethodId")
    List<Object[]> getStatisticsByPaymentMethod();

    /**
     * Gets transaction statistics by status
     *
     * @return list of [status, count, totalAmount]
     */
    @Query("SELECT pt.status, COUNT(pt), COALESCE(SUM(pt.amount), 0) FROM PaymentTransaction pt GROUP BY pt.status")
    List<Object[]> getStatisticsByStatus();

    // =========================================================================
    // UPDATE QUERIES
    // =========================================================================

    /**
     * Updates transaction status
     *
     * @param id transaction ID
     * @param status new status
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE PaymentTransaction pt SET pt.status = :status, pt.updatedAt = CURRENT_TIMESTAMP WHERE pt.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") PaymentTransactionStatus status);

    /**
     * Completes transaction with provider ID
     *
     * @param id transaction ID
     * @param providerTransactionId provider's transaction ID
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE PaymentTransaction pt SET pt.status = 'COMPLETED', pt.providerTransactionId = :providerTransactionId, pt.completedAt = CURRENT_TIMESTAMP, pt.updatedAt = CURRENT_TIMESTAMP WHERE pt.id = :id")
    int completeTransaction(@Param("id") Long id, @Param("providerTransactionId") String providerTransactionId);

    /**
     * Fails transaction with error message
     *
     * @param id transaction ID
     * @param errorMessage error description
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE PaymentTransaction pt SET pt.status = 'FAILED', pt.errorMessage = :errorMessage, pt.updatedAt = CURRENT_TIMESTAMP WHERE pt.id = :id")
    int failTransaction(@Param("id") Long id, @Param("errorMessage") String errorMessage);

    /**
     * Refunds completed transaction
     *
     * @param id transaction ID
     * @param refundTransactionId refund transaction ID
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE PaymentTransaction pt SET pt.status = 'REFUNDED', pt.providerTransactionId = :refundTransactionId, pt.updatedAt = CURRENT_TIMESTAMP WHERE pt.id = :id")
    int refundTransaction(@Param("id") Long id, @Param("refundTransactionId") String refundTransactionId);

    // =========================================================================
    // EXISTENCE CHECKS
    // =========================================================================

    /**
     * Checks if invoice has any completed transactions
     *
     * @param invoiceId invoice ID
     * @return true if it has completed transactions
     */
    boolean existsByInvoiceIdAndStatus(Long invoiceId, PaymentTransactionStatus status);

    /**
     * Checks if order has any completed transactions
     *
     * @param orderId order ID
     * @param orderType order type
     * @return true if it has completed transactions
     */
    boolean existsByOrderIdAndOrderTypeAndStatus(Long orderId, OrderType orderType, PaymentTransactionStatus status);

    /**
     * Check if a refund already exists for the original transaction
     *
     * @param originalTransactionId original transaction ID
     * @return true if refund exists
     */
    boolean existsByOriginalTransactionId(Long originalTransactionId);

    /**
     * Find refund transaction by original transaction ID
     *
     * @param originalTransactionId original transaction ID
     * @return optional refund transaction
     */
    Optional<PaymentTransaction> findByOriginalTransactionId(Long originalTransactionId);

    /**
     * Find all refunds for an original transaction
     *
     * @param originalTransactionId original transaction ID
     * @return list of refund transactions
     */
    List<PaymentTransaction> findAllByOriginalTransactionId(Long originalTransactionId);

    /**
     * Counts transactions created by a user
     *
     * @param userId user ID
     * @return total count of transactions
     */
    long countByCreatedBy(Long userId);
}