package ru.galtor85.household_store.repository.cash;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.finance.CashTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for cash transaction operations.
 */
@Repository
public interface CashTransactionRepository extends JpaRepository<CashTransaction, Long> {

    /**
     * Finds paginated transactions by cash register ID.
     *
     * @param cashRegisterId cash register ID
     * @param pageable       pagination information
     * @return page of cash transactions
     */
    Page<CashTransaction> findByCashRegisterId(Long cashRegisterId, Pageable pageable);

    /**
     * Finds all transactions for an invoice.
     *
     * @param invoiceId invoice ID
     * @return list of cash transactions
     */
    List<CashTransaction> findByInvoiceId(Long invoiceId);

    /**
     * Finds transactions for an invoice ordered chronologically.
     *
     * @param invoiceId invoice ID
     * @return list of cash transactions sorted by creation date
     */
    @Query("SELECT ct FROM CashTransaction ct WHERE ct.invoice.id = :invoiceId ORDER BY ct.createdAt ASC")
    List<CashTransaction> findByInvoiceIdOrdered(@Param("invoiceId") Long invoiceId);

    /**
     * Finds transactions by cash register within date range.
     *
     * @param cashRegisterId cash register ID
     * @param startDate      period start
     * @param endDate        period end
     * @return list of cash transactions
     */
    @Query("SELECT ct FROM CashTransaction ct WHERE ct.cashRegister.id = :cashRegisterId " +
            "AND ct.createdAt BETWEEN :startDate AND :endDate")
    List<CashTransaction> findByCashRegisterIdAndDateRange(@Param("cashRegisterId") Long cashRegisterId,
                                                           @Param("startDate") LocalDateTime startDate,
                                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Gets total income for cash register within date range.
     *
     * @param cashRegisterId cash register ID
     * @param startDate      period start
     * @param endDate        period end
     * @return total income amount
     */
    @Query("SELECT COALESCE(SUM(ct.amount), 0) FROM CashTransaction ct " +
            "WHERE ct.cashRegister.id = :cashRegisterId " +
            "AND ct.transactionType = 'INCOME' " +
            "AND ct.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalIncomeByCashRegisterAndDateRange(@Param("cashRegisterId") Long cashRegisterId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Gets total expense for cash register within date range.
     *
     * @param cashRegisterId cash register ID
     * @param startDate      period start
     * @param endDate        period end
     * @return total expense amount
     */
    @Query("SELECT COALESCE(SUM(ct.amount), 0) FROM CashTransaction ct " +
            "WHERE ct.cashRegister.id = :cashRegisterId " +
            "AND ct.transactionType = 'EXPENSE' " +
            "AND ct.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpenseByCashRegisterAndDateRange(@Param("cashRegisterId") Long cashRegisterId,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);

      /**
     * Checks if a refund transaction exists for the original transaction.
     *
     * @param transactionId original transaction ID
     * @return true if refund exists
     */
    @Query("SELECT COUNT(ct) > 0 FROM CashTransaction ct WHERE ct.originalTransactionId = :transactionId")
    boolean existsByOriginalTransactionId(@Param("transactionId") Long transactionId);

    boolean existsByCashRegisterId(Long cashRegisterId);

    /**
     * Gets total refund amount TO customers (SALES orders)
     */
    @Query("SELECT COALESCE(SUM(ct.amount), 0) FROM CashTransaction ct " +
            "WHERE ct.cashRegister.id = :cashRegisterId " +
            "AND ct.transactionType = 'REFUND' " +
            "AND ct.invoice.salesOrderId IS NOT NULL " +
            "AND ct.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRefundToCustomerByCashRegisterAndDateRange(@Param("cashRegisterId") Long cashRegisterId,
                                                                  @Param("startDate") LocalDateTime startDate,
                                                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Gets total refund amount FROM suppliers (PURCHASE orders)
     */
    @Query("SELECT COALESCE(SUM(ct.amount), 0) FROM CashTransaction ct " +
            "WHERE ct.cashRegister.id = :cashRegisterId " +
            "AND ct.transactionType = 'REFUND' " +
            "AND ct.invoice.purchaseOrderId IS NOT NULL " +
            "AND ct.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRefundFromSupplierByCashRegisterAndDateRange(@Param("cashRegisterId") Long cashRegisterId,
                                                                    @Param("startDate") LocalDateTime startDate,
                                                                    @Param("endDate") LocalDateTime endDate);
}