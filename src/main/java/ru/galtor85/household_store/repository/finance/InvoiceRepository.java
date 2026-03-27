package ru.galtor85.household_store.repository.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.finance.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // =========================================================================
    // БАЗОВЫЕ ЗАПРОСЫ
    // =========================================================================

    /**
     * Находит счет по номеру
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Находит все счета по статусу
     */
    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    /**
     * Находит все счета по способу оплаты
     */
    List<Invoice> findByPaymentMethod(PaymentMethod paymentMethod);

    // =========================================================================
    // ЗАПРОСЫ ПО ЗАКАЗАМ
    // =========================================================================

    /**
     * Находит счета для заказа на закупку
     */
    List<Invoice> findByPurchaseOrderId(Long purchaseOrderId);

    /**
     * Находит счета для заказа на продажу
     */
    List<Invoice> findBySalesOrderId(Long salesOrderId);

    /**
     * Находит счета для заказа на закупку с пагинацией
     */
    Page<Invoice> findByPurchaseOrderId(Long purchaseOrderId, Pageable pageable);

    /**
     * Находит счета для заказа на продажу с пагинацией
     */
    Page<Invoice> findBySalesOrderId(Long salesOrderId, Pageable pageable);

    // =========================================================================
    // ЗАПРОСЫ ПО ПРОСРОЧКЕ
    // =========================================================================

    /**
     * Находит просроченные счета (не оплачены, срок оплаты истек)
     */
    @Query("SELECT i FROM Invoice i WHERE i.dueDate < :now " +
            "AND i.status = 'PENDING'")
    List<Invoice> findOverdueInvoices(@Param("now") LocalDateTime now);

    /**
     * Находит счета, которые должны быть оплачены в ближайшие N дней
     */
    @Query("SELECT i FROM Invoice i WHERE i.dueDate BETWEEN :now AND :endDate " +
            "AND i.status = 'PENDING'")
    List<Invoice> findUpcomingInvoices(@Param("now") LocalDateTime now,
                                       @Param("endDate") LocalDateTime endDate);

    // =========================================================================
    // СТАТИСТИЧЕСКИЕ ЗАПРОСЫ
    // =========================================================================

    /**
     * Получает общую сумму ожидающих оплаты счетов
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i WHERE i.status = 'PENDING'")
    BigDecimal getTotalPendingAmount();

    /**
     * Получает общую сумму оплаченных счетов за период
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i " +
            "WHERE i.status = 'PAID' AND i.paidDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalPaidAmountForPeriod(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Получает общую сумму по статусам
     */
    @Query("SELECT i.status, COALESCE(SUM(i.amount), 0) FROM Invoice i GROUP BY i.status")
    List<Object[]> getTotalAmountByStatus();

    /**
     * Получает сумму по способам оплаты
     */
    @Query("SELECT i.paymentMethod, COALESCE(SUM(i.amount), 0) FROM Invoice i " +
            "WHERE i.status = 'PAID' GROUP BY i.paymentMethod")
    List<Object[]> getTotalAmountByPaymentMethod();

    // =========================================================================
    // ЗАПРОСЫ ПО ДАТАМ
    // =========================================================================

    /**
     * Получает количество счетов за период
     */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.issueDate BETWEEN :startDate AND :endDate")
    long countByIssueDateBetween(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Получает сумму счетов за период
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i " +
            "WHERE i.issueDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalAmountByIssueDateBetween(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    // =========================================================================
    // ОБНОВЛЕНИЯ
    // =========================================================================

    /**
     * Обновляет статус счета
     */
    @Modifying
    @Query("UPDATE Invoice i SET i.status = :status, i.paidDate = :paidDate " +
            "WHERE i.id = :invoiceId")
    int updateInvoiceStatus(@Param("invoiceId") Long invoiceId,
                            @Param("status") InvoiceStatus status,
                            @Param("paidDate") LocalDateTime paidDate);

    /**
     * Отмечает счет как оплаченный
     */
    default int markAsPaid(Long invoiceId) {
        return updateInvoiceStatus(invoiceId, InvoiceStatus.PAID, LocalDateTime.now());
    }

    /**
     * Отменяет счет
     */
    @Modifying
    @Query("UPDATE Invoice i SET i.status = 'CANCELLED' WHERE i.id = :invoiceId")
    int cancelInvoice(@Param("invoiceId") Long invoiceId);

    // =========================================================================
    // ПРОВЕРКИ
    // =========================================================================

    /**
     * Проверяет, существует ли счет с таким номером
     */
    boolean existsByInvoiceNumber(String invoiceNumber);

    /**
     * Проверяет, есть ли у заказа на закупку неоплаченные счета
     */
    @Query("SELECT COUNT(i) > 0 FROM Invoice i WHERE i.purchaseOrderId = :orderId " +
            "AND i.status IN ('PENDING', 'PARTIALLY_PAID')")
    boolean hasUnpaidInvoicesForPurchaseOrder(@Param("orderId") Long orderId);

    /**
     * Проверяет, есть ли у заказа на продажу неоплаченные счета
     */
    @Query("SELECT COUNT(i) > 0 FROM Invoice i WHERE i.salesOrderId = :orderId " +
            "AND i.status IN ('PENDING', 'PARTIALLY_PAID')")
    boolean hasUnpaidInvoicesForSalesOrder(@Param("orderId") Long orderId);
}