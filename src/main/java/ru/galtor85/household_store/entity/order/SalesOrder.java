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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity for sales orders.
 *
 * <p>Represents a customer order in the system. Contains order items,
 * pricing information, delivery details, payment status, and reservation
 * information for cash payments.</p>
 *
 * @author G@LTor85
 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sales_orders", schema = "household_schema")
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    @Builder.Default
    private SalesOrderType orderType = SalesOrderType.RETAIL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SalesOrderItem> items = new ArrayList<>();

    // =========================================================================
    // FINANCIAL FIELDS
    // =========================================================================

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "shipping_amount", precision = 10, scale = 2)
    private BigDecimal shippingAmount;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;

    // =========================================================================
    // PAYMENT FIELDS
    // =========================================================================

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_details")
    private String paymentDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private OrderPaymentStatus paymentStatus;

    // =========================================================================
    // DELIVERY FIELDS
    // =========================================================================

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Column(name = "billing_address")
    private String billingAddress;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "estimated_delivery")
    private LocalDateTime estimatedDelivery;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    // =========================================================================
    // CANCELLATION FIELDS
    // =========================================================================

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    // =========================================================================
    // RESERVATION FIELDS (for cash payments)
    // =========================================================================

    @Column(name = "reserved_until")
    private LocalDateTime reservedUntil;

    @Column(name = "reservation_status")
    @Enumerated(EnumType.STRING)
    private ReservationStatus reservationStatus;

    // =========================================================================
    // AUDIT FIELDS
    // =========================================================================

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "notes")
    private String notes;

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

    // =========================================================================
    // RELATIONSHIPS
    // =========================================================================

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Invoice> invoices = new ArrayList<>();

    // =========================================================================
    // BUSINESS METHODS
    // =========================================================================

    /**
     * Adds an invoice to this sales order.
     *
     * @param invoice the invoice to add
     */
    public void addInvoice(Invoice invoice) {
        invoices.add(invoice);
        invoice.setSalesOrder(this);
    }

    /**
     * Adds an item to the order and recalculates totals.
     *
     * @param item the order item to add
     */
    public void addItem(SalesOrderItem item) {
        items.add(item);
        item.setSalesOrder(this);
        recalculateTotals();
    }

    /**
     * Recalculates subtotal and total amounts based on current items.
     */
    public void recalculateTotals() {
        this.subtotal = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalAmount = this.subtotal
                .add(this.shippingAmount != null ? this.shippingAmount : BigDecimal.ZERO)
                .add(this.taxAmount != null ? this.taxAmount : BigDecimal.ZERO)
                .subtract(this.discountAmount != null ? this.discountAmount : BigDecimal.ZERO);
    }

    /**
     * Checks if order is wholesale type.
     *
     * @return true if wholesale
     */
    public boolean isWholesale() {
        return orderType == SalesOrderType.WHOLESALE;
    }

    // =========================================================================
    // INNER ENUMS
    // =========================================================================

    /**
     * Reservation status for cash payments.
     */
    public enum ReservationStatus {
        /** Products are reserved for customer */
        ACTIVE,
        /** Reservation period has expired */
        EXPIRED,
        /** Products were paid and received */
        COMPLETED
    }
}