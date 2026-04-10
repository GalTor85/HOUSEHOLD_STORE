package ru.galtor85.household_store.entity.payment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Base entity for all payment methods
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_methods", schema = "household_schema")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "method_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethodType methodType;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "is_default")
    private boolean isDefault = false;

    @Column(nullable = false)
    private String currency = "RUB";

    @Column(name = "processing_fee", precision = 5, scale = 2)
    private BigDecimal processingFee = BigDecimal.ZERO;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public abstract boolean validate();
    public abstract String getMaskedIdentifier();
    public abstract PaymentProvider getProvider();
}