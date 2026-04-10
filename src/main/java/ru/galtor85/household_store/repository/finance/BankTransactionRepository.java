package ru.galtor85.household_store.repository.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.finance.BankTransaction;
import ru.galtor85.household_store.entity.finance.BankTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for BankTransaction entity
 */
@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    // =========================================================================
    // SEARCH BY ACCOUNT
    // =========================================================================

    /**
     * Finds all transactions for a bank account
     *
     * @param bankAccountId bank account ID
     * @return list of transactions
     */
    List<BankTransaction> findByBankAccountId(Long bankAccountId);

    /**
     * Finds paginated transactions for a bank account
     *
     * @param bankAccountId bank account ID
     * @param pageable pagination info
     * @return page of transactions
     */
    Page<BankTransaction> findByBankAccountId(Long bankAccountId, Pageable pageable);

    /**
     * Finds transactions by type for a bank account
     *
     * @param bankAccountId bank account ID
     * @param type transaction type
     * @return list of transactions
     */
    List<BankTransaction> findByBankAccountIdAndType(Long bankAccountId, BankTransactionType type);

    // =========================================================================
    // SEARCH BY REFERENCE
    // =========================================================================

    /**
     * Finds transactions by reference
     *
     * @param referenceId reference ID
     * @param referenceType reference type
     * @return list of transactions
     */
    List<BankTransaction> findByReferenceIdAndReferenceType(Long referenceId, String referenceType);

    /**
     * Finds transactions by invoice ID
     *
     * @param invoiceId invoice ID
     * @return list of transactions
     */
    @Query("SELECT bt FROM BankTransaction bt WHERE bt.referenceId = :invoiceId AND bt.referenceType = 'INVOICE'")
    List<BankTransaction> findByInvoiceId(@Param("invoiceId") Long invoiceId);

    /**
     * Finds transactions by order ID
     *
     * @param orderId order ID
     * @param orderType order type (PURCHASE_ORDER, SALES_ORDER)
     * @return list of transactions
     */
    @Query("SELECT bt FROM BankTransaction bt WHERE bt.referenceId = :orderId AND bt.referenceType = :orderType")
    List<BankTransaction> findByOrderId(@Param("orderId") Long orderId, @Param("orderType") String orderType);

    // =========================================================================
    // SEARCH BY TRANSFER
    // =========================================================================

    /**
     * Finds all transfers from a specific account
     *
     * @param fromAccountId source account ID
     * @return list of transactions
     */
    List<BankTransaction> findByFromAccountId(Long fromAccountId);

    /**
     * Finds all transfers to a specific account
     *
     * @param toAccountId destination account ID
     * @return list of transactions
     */
    List<BankTransaction> findByToAccountId(Long toAccountId);

    /**
     * Finds transfers between two accounts
     *
     * @param fromAccountId source account ID
     * @param toAccountId destination account ID
     * @return list of transactions
     */
    List<BankTransaction> findByFromAccountIdAndToAccountId(Long fromAccountId, Long toAccountId);

    // =========================================================================
    // DATE RANGE SEARCH
    // =========================================================================

    /**
     * Finds transactions within date range
     *
     * @param startDate start date
     * @param endDate end date
     * @return list of transactions
     */
    List<BankTransaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Finds transactions for an account within date range
     *
     * @param bankAccountId bank account ID
     * @param startDate start date
     * @param endDate end date
     * @return list of transactions
     */
    List<BankTransaction> findByBankAccountIdAndCreatedAtBetween(Long bankAccountId,
                                                                 LocalDateTime startDate,
                                                                 LocalDateTime endDate);

    // =========================================================================
    // AGGREGATION QUERIES
    // =========================================================================

    /**
     * Gets total deposited amount for an account
     *
     * @param bankAccountId bank account ID
     * @return total deposited amount
     */
    @Query("SELECT COALESCE(SUM(bt.amount), 0) FROM BankTransaction bt WHERE bt.bankAccountId = :bankAccountId AND bt.type = 'DEPOSIT'")
    BigDecimal getTotalDeposited(@Param("bankAccountId") Long bankAccountId);

    /**
     * Gets total withdrawn amount for an account
     *
     * @param bankAccountId bank account ID
     * @return total withdrawn amount
     */
    @Query("SELECT COALESCE(SUM(bt.amount), 0) FROM BankTransaction bt WHERE bt.bankAccountId = :bankAccountId AND bt.type = 'WITHDRAW'")
    BigDecimal getTotalWithdrawn(@Param("bankAccountId") Long bankAccountId);

    /**
     * Gets net change for an account within date range
     *
     * @param bankAccountId bank account ID
     * @param startDate start date
     * @param endDate end date
     * @return net change
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN bt.type = 'DEPOSIT' THEN bt.amount ELSE -bt.amount END), 0) " +
            "FROM BankTransaction bt WHERE bt.bankAccountId = :bankAccountId " +
            "AND bt.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getNetChangeForPeriod(@Param("bankAccountId") Long bankAccountId,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    // =========================================================================
    // LATEST TRANSACTIONS
    // =========================================================================

    /**
     * Finds latest transaction for an account
     *
     * @param bankAccountId bank account ID
     * @return latest transaction (optional)
     */
    @Query("SELECT bt FROM BankTransaction bt WHERE bt.bankAccountId = :bankAccountId ORDER BY bt.createdAt DESC")
    Optional<BankTransaction> findLatestByAccountId(@Param("bankAccountId") Long bankAccountId);

    /**
     * Finds latest N transactions for an account
     *
     * @param bankAccountId bank account ID
     * @param limit maximum number of transactions to return
     * @return list of latest transactions
     */
    @Query(value = "SELECT * FROM household_schema.bank_transactions WHERE bank_account_id = :bankAccountId ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<BankTransaction> findLatestByAccountIdWithLimit(@Param("bankAccountId") Long bankAccountId, @Param("limit") int limit);
}