package ru.galtor85.household_store.entity.finance;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.SalesOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Invoice entity.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@FieldNameConstants
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

    @Builder.Default
    @Column(name = "deleted")
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Column(name = "delete_reason")
    private String deleteReason;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CashTransaction> cashTransactions = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", insertable = false, updatable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", insertable = false, updatable = false)
    private SalesOrder salesOrder;

    public boolean isPurchaseOrder() {
        return purchaseOrderId != null;
    }

    public boolean isSalesOrder() {
        return salesOrderId != null;
    }

    public boolean isPayable() {
        return status == InvoiceStatus.PENDING || status == InvoiceStatus.PARTIALLY_PAID;
    }

    public boolean isNotPayable() {
        return !isPayable();
    }


    public boolean isCancellable() {
        return status == InvoiceStatus.PENDING || status == InvoiceStatus.PARTIALLY_PAID;
    }

    public void markAsPaid() {
        this.status = InvoiceStatus.PAID;
        this.paidDate = LocalDateTime.now();
    }

    public BigDecimal getTotalPaidAmount() {
        return cashTransactions.stream()
                .map(t -> t.getTransactionType() == TransactionType.REFUND
                        ? t.getAmount().negate()
                        : t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getRemainingAmount() {
        return amount.subtract(getTotalPaidAmount());
    }
}