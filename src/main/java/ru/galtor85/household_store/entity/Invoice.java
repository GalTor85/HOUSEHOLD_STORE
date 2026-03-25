package ru.galtor85.household_store.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoices", schema = "household_schema")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "purchase_order_id")
    private Long purchaseOrderId;

    @Column(name = "sales_order_id")
    private Long salesOrderId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "paid_date")
    private LocalDateTime paidDate;

    @Column(length = 500)
    private String description;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CashTransaction> cashTransactions = new ArrayList<>();

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    public boolean isPurchaseOrder() {
        return purchaseOrderId != null;
    }

    public boolean isSalesOrder() {
        return salesOrderId != null;
    }

    public Long getOrderId() {
        return purchaseOrderId != null ? purchaseOrderId : salesOrderId;
    }

    /**
     * Проверяет, оплачен ли счет
     */
    public boolean isPaid() {
        return status == InvoiceStatus.PAID;
    }

    /**
     * Проверяет, можно ли оплатить счет
     */
    public boolean isPayable() {
        return status == InvoiceStatus.PENDING || status == InvoiceStatus.PARTIALLY_PAID;
    }

    /**
     * Проверяет, можно ли отменить счет
     */
    public boolean isCancellable() {
        return status == InvoiceStatus.PENDING || status == InvoiceStatus.PARTIALLY_PAID;
    }

    /**
     * Проверяет, просрочен ли счет
     */
    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) && !isPaid();
    }

    /**
     * Отмечает счет как оплаченный
     */
    public void markAsPaid() {
        this.status = InvoiceStatus.PAID;
        this.paidDate = LocalDateTime.now();
    }

    /**
     * Отмечает счет как частично оплаченный
     */
    public void markAsPartiallyPaid() {
        if (this.status != InvoiceStatus.PAID) {
            this.status = InvoiceStatus.PARTIALLY_PAID;
        }
    }

    /**
     * Отменяет счет
     */
    public void cancel() {
        this.status = InvoiceStatus.CANCELLED;
    }

    /**
     * Добавляет кассовую операцию
     */
    public void addCashTransaction(CashTransaction transaction) {
        cashTransactions.add(transaction);
        transaction.setInvoice(this);
    }

    /**
     * Получает общую сумму оплаченных операций
     */
    public BigDecimal getTotalPaidAmount() {
        return cashTransactions.stream()
                .filter(ct -> ct.getTransactionType() == TransactionType.INCOME)
                .map(CashTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Получает остаток к оплате
     */
    public BigDecimal getRemainingAmount() {
        return amount.subtract(getTotalPaidAmount());
    }
}