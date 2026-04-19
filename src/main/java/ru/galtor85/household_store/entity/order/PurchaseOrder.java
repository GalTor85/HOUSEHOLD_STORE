package ru.galtor85.household_store.entity.order;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.galtor85.household_store.entity.finance.Invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_orders", schema = "household_schema")
public class PurchaseOrder {

    // =========================================================================
    // IDENTIFIERS
    // =========================================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    // =========================================================================
    // ORDER TYPE AND STATUS
    // =========================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    @Builder.Default
    private PurchaseOrderType orderType = PurchaseOrderType.PURCHASE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // =========================================================================
    // ORDER ITEMS
    // =========================================================================

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PurchaseOrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Invoice> invoices = new ArrayList<>();

    // =========================================================================
    // DELIVERY INFORMATION
    // =========================================================================

    @Column(name = "expected_delivery")
    private LocalDate expectedDelivery;

    @Column(name = "actual_delivery")
    private LocalDate actualDelivery;

    @Column(name = "warehouse_location")
    private String warehouseLocation;

    @Column(name = "received_by")
    private Long receivedBy;

    @Column(name = "quality_check")
    private Boolean qualityCheck;

    // =========================================================================
    // PAYMENT INFORMATION
    // =========================================================================

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "payment_due")
    private LocalDate paymentDue;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private OrderPaymentStatus paymentStatus;

    // =========================================================================
    // FINANCIAL FIELDS
    // =========================================================================

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // =========================================================================
    // ADMINISTRATIVE FIELDS
    // =========================================================================

    @Column(name = "created_by")
    private Long createdBy;

    @Column(length = 3000)
    private String notes;

    @Column(name = "deleted")
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Column(name = "delete_reason")
    private String deleteReason;

    // =========================================================================
    // CANCELLATION FIELDS
    // =========================================================================

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;

    // =========================================================================
    // TIMESTAMPS
    // =========================================================================

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Adds an invoice to this purchase order
     *
     * @param invoice the invoice to add
     */
    public void addInvoice(Invoice invoice) {
        invoices.add(invoice);
        invoice.setPurchaseOrder(this);
    }
}