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
 * Entity for all payment methods
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_methods", schema = "household_schema")
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "method_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethodType methodType;

    @Column(name = "provider")
    @Enumerated(EnumType.STRING)
    private PaymentProvider provider;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_default")
    @Builder.Default
    private boolean isDefault = false;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "RUB";

    @Column(name = "processing_fee", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal processingFee = BigDecimal.ZERO;

    @Column(name = "masked_identifier")
    private String maskedIdentifier;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean validate() {
        return true;
    }

    public String getMaskedIdentifier() {
        if (maskedIdentifier != null && !maskedIdentifier.isEmpty()) {
            return maskedIdentifier;
        }
        return "***";
    }

    public PaymentProvider getProvider() {
        return provider != null ? provider : PaymentProvider.SBERBANK;
    }
}