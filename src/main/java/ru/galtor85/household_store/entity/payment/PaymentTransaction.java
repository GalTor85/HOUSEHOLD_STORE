package ru.galtor85.household_store.entity.payment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.galtor85.household_store.entity.order.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment transaction record
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_transactions", schema = "household_schema")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_method_id", nullable = false)
    private Long paymentMethodId;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "order_id")
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type")
    private OrderType orderType; // PURCHASE, SALES

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentTransactionStatus status;

    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    @Column(name = "provider_payment_url")
    private String providerPaymentUrl;

    @Column(length = 1000)
    private String description;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "processing_fee", precision = 10, scale = 2)
    private BigDecimal processingFee;

    @Column(name = "net_amount", precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "original_transaction_id")
    private Long originalTransactionId;

    @Column(name = "refund_transaction_id")
    private Long refundTransactionId;
}