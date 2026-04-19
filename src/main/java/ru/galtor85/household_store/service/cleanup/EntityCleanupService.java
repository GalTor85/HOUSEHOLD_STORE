package ru.galtor85.household_store.service.cleanup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.config.BusinessConfig;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.payment.PaymentTransaction;
import ru.galtor85.household_store.entity.payment.PaymentTransactionStatus;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.repository.order.PurchaseOrderRepository;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.repository.payment.PaymentTransactionRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;

/**
 * Service for managing soft deletion and cleanup of entities.
 * <p>
 * Provides soft delete functionality for:
 * <ul>
 *   <li>Sales orders</li>
 *   <li>Purchase orders</li>
 *   <li>Invoices</li>
 *   <li>Payment transactions</li>
 * </ul>
 * Soft delete marks entities as deleted without physically removing them from the database.
 * This allows for recovery if needed and maintains referential integrity.
 * </p>
 * <p>
 * The service also provides automatic cleanup of expired deleted entities based on
 * configurable retention periods from {@link BusinessConfig.CleanupConfig}.
 * </p>
 *
 * @author G@LTor85
 * @see BusinessConfig.CleanupConfig
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityCleanupService {

    private final SalesOrderRepository salesOrderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final BusinessConfig businessConfig;
    private final MessageService messageService;
    private final LogMessageService logMsg;

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Gets retention period in days for soft-deleted entities.
     *
     * @return retention days (default: 90)
     */
    private int getRetentionDays() {
        Integer days = businessConfig.getCleanup().getRetentionDays();
        return days != null ? days : 90;
    }

    /**
     * Gets how many months back to check for recent payments.
     * Orders with payments within this period cannot be deleted.
     *
     * @return months for recent payments check (default: 6)
     */
    private int getRecentPaymentsMonths() {
        Integer months = businessConfig.getCleanup().getRecentPaymentsMonths();
        return months != null ? months : 6;
    }

    /**
     * Gets minimum age in months for invoice deletion.
     * Invoices younger than this cannot be deleted.
     *
     * @return minimum invoice age in months (default: 6)
     */
    private int getInvoiceMinAgeMonths() {
        Integer months = businessConfig.getCleanup().getInvoiceMinAgeMonths();
        return months != null ? months : 6;
    }

    /**
     * Gets minimum age in months for purchase order deletion.
     * Purchase orders younger than this cannot be deleted.
     *
     * @return minimum purchase order age in months (default: 6)
     */
    private int getPurchaseOrderMinAgeMonths() {
        Integer months = businessConfig.getCleanup().getPurchaseOrderMinAgeMonths();
        return months != null ? months : 6;
    }

    /**
     * Gets minimum age in months for payment transaction deletion.
     * Transactions younger than this cannot be deleted.
     *
     * @return minimum transaction age in months (default: 3)
     */
    private int getPaymentMinAgeMonths() {
        Integer months = businessConfig.getCleanup().getPaymentMinAgeMonths();
        return months != null ? months : 3;
    }

    // =========================================================================
    // SALES ORDER SOFT DELETE
    // =========================================================================

    /**
     * Soft deletes a sales order by ID.
     * <p>
     * Validation rules:
     * <ul>
     *   <li>Order must exist</li>
     *   <li>Order must not be already deleted</li>
     *   <li>Order status must be CANCELLED or COMPLETED</li>
     *   <li>Order must not have recent payments (within configured months)</li>
     * </ul>
     * </p>
     *
     * @param orderId   ID of the sales order to delete
     * @param reason    reason for deletion
     * @param deletedBy ID of the user performing the deletion
     * @throws IllegalArgumentException if order not found
     * @throws IllegalStateException    if order is already deleted, has wrong status, or has recent payments
     */
    @Transactional
    public void softDeleteSalesOrder(Long orderId, String reason, Long deletedBy) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("cleanup.sales.order.not.found", orderId)));

        if (order.isDeleted()) {
            throw new IllegalStateException(
                    messageService.get("cleanup.sales.order.already.deleted", orderId));
        }

        if (order.getStatus() != OrderStatus.CANCELLED &&
                order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException(
                    messageService.get("cleanup.sales.order.cannot.delete.not.cancelled.or.completed", orderId));
        }

        boolean hasRecentPayments = paymentTransactionRepository.existsByOrderIdAndCreatedAtAfter(
                orderId, LocalDateTime.now().minusMonths(getRecentPaymentsMonths()));

        if (hasRecentPayments) {
            throw new IllegalStateException(
                    messageService.get("cleanup.sales.order.cannot.delete.has.recent.payments", orderId));
        }

        order.setDeleted(true);
        order.setDeletedAt(LocalDateTime.now());
        order.setDeletedBy(deletedBy);
        order.setDeleteReason(reason);

        salesOrderRepository.save(order);
        log.info(logMsg.get("entity.cleanup.sales.order.deleted", orderId, reason));
    }

    // =========================================================================
    // PURCHASE ORDER SOFT DELETE
    // =========================================================================

    /**
     * Soft deletes a purchase order by ID.
     * <p>
     * Validation rules:
     * <ul>
     *   <li>Order must exist</li>
     *   <li>Order must not be already deleted</li>
     *   <li>Order status must be CANCELLED</li>
     *   <li>Order must not have any non-cancelled invoices</li>
     *   <li>Order must be older than configured minimum age (default: 6 months)</li>
     * </ul>
     * </p>
     *
     * @param orderId   ID of the purchase order to delete
     * @param reason    reason for deletion
     * @param deletedBy ID of the user performing the deletion
     * @throws IllegalArgumentException if order not found
     * @throws IllegalStateException    if order is already deleted, has wrong status, has payments, or is too recent
     */
    @Transactional
    public void softDeletePurchaseOrder(Long orderId, String reason, Long deletedBy) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("cleanup.purchase.order.not.found", orderId)));

        if (order.isDeleted()) {
            throw new IllegalStateException(
                    messageService.get("cleanup.purchase.order.already.deleted", orderId));
        }

        if (order.getStatus() != OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    messageService.get("cleanup.purchase.order.cannot.delete.not.cancelled", orderId));
        }

        boolean hasPayments = invoiceRepository.existsByPurchaseOrderIdAndStatusNot(
                orderId, InvoiceStatus.CANCELLED);

        if (hasPayments) {
            throw new IllegalStateException(
                    messageService.get("cleanup.purchase.order.cannot.delete.has.payments", orderId));
        }

        if (order.getCreatedAt().isAfter(LocalDateTime.now().minusMonths(getPurchaseOrderMinAgeMonths()))) {
            throw new IllegalStateException(
                    messageService.get("cleanup.purchase.order.cannot.delete.too.recent", orderId));
        }

        order.setDeleted(true);
        order.setDeletedAt(LocalDateTime.now());
        order.setDeletedBy(deletedBy);
        order.setDeleteReason(reason);

        purchaseOrderRepository.save(order);
        log.info(logMsg.get("entity.cleanup.purchase.order.deleted", orderId, reason));
    }

    // =========================================================================
    // INVOICE SOFT DELETE
    // =========================================================================

    /**
     * Soft deletes an invoice by ID.
     * <p>
     * Validation rules:
     * <ul>
     *   <li>Invoice must exist</li>
     *   <li>Invoice must not be already deleted</li>
     *   <li>Invoice status must be CANCELLED or REFUNDED</li>
     *   <li>Invoice must be older than configured minimum age (default: 6 months)</li>
     * </ul>
     * </p>
     *
     * @param invoiceId ID of the invoice to delete
     * @param reason    reason for deletion
     * @param deletedBy ID of the user performing the deletion
     * @throws IllegalArgumentException if invoice not found
     * @throws IllegalStateException    if invoice is already deleted, has wrong status, or is too recent
     */
    @Transactional
    public void softDeleteInvoice(Long invoiceId, String reason, Long deletedBy) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("cleanup.invoice.not.found", invoiceId)));

        if (invoice.isDeleted()) {
            throw new IllegalStateException(
                    messageService.get("cleanup.invoice.already.deleted", invoiceId));
        }

        if (invoice.getStatus() != InvoiceStatus.CANCELLED &&
                invoice.getStatus() != InvoiceStatus.REFUNDED) {
            throw new IllegalStateException(
                    messageService.get("cleanup.invoice.cannot.delete.not.cancelled", invoiceId));
        }

        if (invoice.getIssueDate().isAfter(LocalDateTime.now().minusMonths(getInvoiceMinAgeMonths()))) {
            throw new IllegalStateException(
                    messageService.get("cleanup.invoice.cannot.delete.too.recent", invoiceId));
        }

        invoice.setDeleted(true);
        invoice.setDeletedAt(LocalDateTime.now());
        invoice.setDeletedBy(deletedBy);
        invoice.setDeleteReason(reason);

        invoiceRepository.save(invoice);
        log.info(logMsg.get("entity.cleanup.invoice.deleted", invoiceId, reason));
    }

    // =========================================================================
    // PAYMENT TRANSACTION SOFT DELETE
    // =========================================================================

    /**
     * Soft deletes a payment transaction by ID.
     * <p>
     * Validation rules:
     * <ul>
     *   <li>Transaction must exist</li>
     *   <li>Transaction must not be already deleted</li>
     *   <li>Transaction status must be FAILED or CANCELLED</li>
     *   <li>Transaction must be older than configured minimum age (default: 3 months)</li>
     * </ul>
     * </p>
     *
     * @param transactionId ID of the payment transaction to delete
     * @param reason        reason for deletion
     * @param deletedBy     ID of the user performing the deletion
     * @throws IllegalArgumentException if transaction not found
     * @throws IllegalStateException    if transaction is already deleted, has wrong status, or is too recent
     */
    @Transactional
    public void softDeletePaymentTransaction(Long transactionId, String reason, Long deletedBy) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("cleanup.payment.not.found", transactionId)));

        if (transaction.isDeleted()) {
            throw new IllegalStateException(
                    messageService.get("cleanup.payment.already.deleted", transactionId));
        }

        if (transaction.getStatus() != PaymentTransactionStatus.FAILED &&
                transaction.getStatus() != PaymentTransactionStatus.CANCELLED) {
            throw new IllegalStateException(
                    messageService.get("cleanup.payment.cannot.delete.not.failed", transactionId));
        }

        if (transaction.getCreatedAt().isAfter(LocalDateTime.now().minusMonths(getPaymentMinAgeMonths()))) {
            throw new IllegalStateException(
                    messageService.get("cleanup.payment.cannot.delete.too.recent", transactionId));
        }

        transaction.setDeleted(true);
        transaction.setDeletedAt(LocalDateTime.now());
        transaction.setDeletedBy(deletedBy);
        transaction.setDeleteReason(reason);

        paymentTransactionRepository.save(transaction);
        log.info(logMsg.get("entity.cleanup.payment.deleted", transactionId, reason));
    }

    // =========================================================================
    // AUTO CLEANUP SCHEDULER
    // =========================================================================

    /**
     * Permanently deletes all soft-deleted entities that have passed the retention period.
     * <p>
     * This method is typically called by a scheduled job.
     * It physically removes records from the database that were soft deleted
     * and are older than the configured retention days (default: 90 days).
     * </p>
     *
     * @return total number of permanently deleted entities across all tables
     */
    @Transactional
    public int cleanupExpiredDeletedEntities() {
        int totalDeleted = 0;

        LocalDateTime threshold = LocalDateTime.now().minusDays(getRetentionDays());

        totalDeleted += purchaseOrderRepository.deleteByDeletedTrueAndDeletedAtBefore(threshold);
        totalDeleted += salesOrderRepository.deleteByDeletedTrueAndDeletedAtBefore(threshold);
        totalDeleted += invoiceRepository.deleteByDeletedTrueAndDeletedAtBefore(threshold);
        totalDeleted += paymentTransactionRepository.deleteByDeletedTrueAndDeletedAtBefore(threshold);

        log.info(logMsg.get("entity.cleanup.auto.completed", totalDeleted));
        return totalDeleted;
    }
}