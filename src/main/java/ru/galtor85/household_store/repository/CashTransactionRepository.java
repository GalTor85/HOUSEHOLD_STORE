package ru.galtor85.household_store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.CashTransaction;
import ru.galtor85.household_store.entity.PaymentMethod;
import ru.galtor85.household_store.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CashTransactionRepository extends JpaRepository<CashTransaction, Long> {

    // =========================================================================
    // БАЗОВЫЕ ЗАПРОСЫ
    // =========================================================================

    List<CashTransaction> findByCashRegisterId(Long cashRegisterId);
    Page<CashTransaction> findByCashRegisterId(Long cashRegisterId, Pageable pageable);

    List<CashTransaction> findByCashierId(Long cashierId);
    List<CashTransaction> findByTransactionType(TransactionType transactionType);
    List<CashTransaction> findByInvoiceId(Long invoiceId);
    Page<CashTransaction> findByInvoiceId(Long invoiceId, Pageable pageable);

    // =========================================================================
    // ЗАПРОСЫ ПО ПЕРИОДУ
    // =========================================================================

    @Query("SELECT ct FROM CashTransaction ct WHERE ct.cashRegister.id = :cashRegisterId " +
            "AND ct.createdAt BETWEEN :startDate AND :endDate")
    List<CashTransaction> findByCashRegisterIdAndDateRange(@Param("cashRegisterId") Long cashRegisterId,
                                                           @Param("startDate") LocalDateTime startDate,
                                                           @Param("endDate") LocalDateTime endDate);

    // =========================================================================
    // СТАТИСТИЧЕСКИЕ ЗАПРОСЫ
    // =========================================================================

    @Query("SELECT COALESCE(SUM(ct.amount), 0) FROM CashTransaction ct " +
            "WHERE ct.cashRegister.id = :cashRegisterId")
    BigDecimal getTotalAmountByCashRegister(@Param("cashRegisterId") Long cashRegisterId);

    @Query("SELECT COALESCE(SUM(ct.amount), 0) FROM CashTransaction ct " +
            "WHERE ct.cashRegister.id = :cashRegisterId " +
            "AND ct.transactionType = 'INCOME' " +
            "AND ct.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalIncomeByCashRegisterAndDateRange(@Param("cashRegisterId") Long cashRegisterId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(ct.amount), 0) FROM CashTransaction ct " +
            "WHERE ct.cashRegister.id = :cashRegisterId " +
            "AND ct.transactionType = 'EXPENSE' " +
            "AND ct.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpenseByCashRegisterAndDateRange(@Param("cashRegisterId") Long cashRegisterId,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(CASE WHEN ct.transactionType = 'INCOME' THEN ct.amount " +
            "WHEN ct.transactionType = 'EXPENSE' THEN -ct.amount ELSE 0 END), 0) " +
            "FROM CashTransaction ct WHERE ct.cashRegister.id = :cashRegisterId " +
            "AND ct.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getNetTurnoverByCashRegisterAndDateRange(@Param("cashRegisterId") Long cashRegisterId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);

    // =========================================================================
    // ПРОВЕРКИ
    // =========================================================================

    /**
     * Проверяет, существует ли возвратная операция для исходной операции
     */
    @Query("SELECT COUNT(ct) > 0 FROM CashTransaction ct WHERE ct.originalTransactionId = :transactionId")
    boolean existsByOriginalTransactionId(@Param("transactionId") Long transactionId);

    /**
     * Проверяет, есть ли операции по кассе
     */
    boolean existsByCashRegisterId(Long cashRegisterId);

    /**
     * Проверяет, есть ли операции по счету
     */
    boolean existsByInvoiceId(Long invoiceId);

    // =========================================================================
    // ПОЛУЧЕНИЕ ПОСЛЕДНЕЙ ОПЕРАЦИИ
    // =========================================================================

    Optional<CashTransaction> findFirstByCashRegisterIdOrderByCreatedAtDesc(Long cashRegisterId);

    // =========================================================================
    // УДАЛЕНИЕ
    // =========================================================================

    @Modifying
    @Query("DELETE FROM CashTransaction ct WHERE ct.cashRegister.id = :cashRegisterId")
    void deleteByCashRegisterId(@Param("cashRegisterId") Long cashRegisterId);

    @Modifying
    @Query("DELETE FROM CashTransaction ct WHERE ct.invoice.id = :invoiceId")
    void deleteByInvoiceId(@Param("invoiceId") Long invoiceId);
}