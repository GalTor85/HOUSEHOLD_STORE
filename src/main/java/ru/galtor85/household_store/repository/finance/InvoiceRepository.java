package ru.galtor85.household_store.repository.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for invoice operations.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Finds invoice by number.
     *
     * @param invoiceNumber invoice number
     * @return optional invoice
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Finds invoices by status with pagination.
     *
     * @param status invoice status
     * @param pageable pagination information
     * @return page of invoices
     */
    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    /**
     * Finds all invoices for a purchase order.
     *
     * @param purchaseOrderId purchase order ID
     * @return list of invoices
     */
    List<Invoice> findByPurchaseOrderId(Long purchaseOrderId);

    /**
     * Finds all invoices for a sales order.
     *
     * @param salesOrderId sales order ID
     * @return list of invoices
     */
    List<Invoice> findBySalesOrderId(Long salesOrderId);

    /**
     * Gets total paid amount for a period.
     *
     * @param startDate period start
     * @param endDate period end
     * @return total paid amount
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i " +
            "WHERE i.status = 'PAID' AND i.paidDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalPaidAmountForPeriod(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Gets total pending amount for purchase orders.
     *
     * @return pending amount for purchase invoices
     */
    @Query("SELECT COALESCE(SUM(i.amount - COALESCE(" +
            "(SELECT SUM(ct.amount) FROM CashTransaction ct " +
            "WHERE ct.invoice.id = i.id AND ct.transactionType = 'EXPENSE'), 0) + " +
            "COALESCE((SELECT SUM(ct.amount) FROM CashTransaction ct " +
            "WHERE ct.invoice.id = i.id AND ct.transactionType = 'REFUND'), 0)), 0) " +
            "FROM Invoice i WHERE i.purchaseOrderId IS NOT NULL " +
            "AND i.status IN ('PENDING', 'PARTIALLY_PAID')")
    BigDecimal getTotalPendingAmountForPurchase();

    /**
     * Gets total pending amount for sales orders.
     *
     * @return pending amount for sales invoices
     */
    @Query("SELECT COALESCE(SUM(i.amount - COALESCE(" +
            "(SELECT SUM(ct.amount) FROM CashTransaction ct " +
            "WHERE ct.invoice.id = i.id AND ct.transactionType = 'INCOME'), 0) + " +
            "COALESCE((SELECT SUM(ct.amount) FROM CashTransaction ct " +
            "WHERE ct.invoice.id = i.id AND ct.transactionType = 'REFUND'), 0)), 0) " +
            "FROM Invoice i WHERE i.salesOrderId IS NOT NULL " +
            "AND i.status IN ('PENDING', 'PARTIALLY_PAID')")
    BigDecimal getTotalPendingAmountForSales();

    /**
     * Checks if invoice number exists.
     *
     * @param invoiceNumber invoice number
     * @return true if exists
     */
    boolean existsByInvoiceNumber(String invoiceNumber);

    /**
     * Finds invoice by ID with cash transactions eagerly loaded.
     *
     * @param id invoice ID
     * @return optional invoice with transactions
     */
    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.cashTransactions WHERE i.id = :id")
    Optional<Invoice> findByIdWithTransactions(@Param("id") Long id);

    @Query("SELECT COUNT(i) > 0 FROM Invoice i " +
            "WHERE i.purchaseOrder.supplierId = :supplierId " +
            "AND i.status IN ('PAID', 'PARTIALLY_PAID')")
    boolean existsPaidBySupplierId(@Param("supplierId") Long supplierId);

    @Query("SELECT COALESCE(SUM(i.amount - COALESCE(" +
            "(SELECT SUM(ct.amount) FROM CashTransaction ct " +
            "WHERE ct.invoice.id = i.id AND ct.transactionType = 'EXPENSE'), 0)), 0) " +
            "FROM Invoice i " +
            "WHERE i.purchaseOrder.supplierId = :supplierId " +
            "AND i.status IN ('PENDING', 'PARTIALLY_PAID')")
    BigDecimal getUnpaidAmountBySupplierId(@Param("supplierId") Long supplierId);
}