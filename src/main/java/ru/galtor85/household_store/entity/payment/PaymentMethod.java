package ru.galtor85.household_store.entity.payment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_CURRENCY_CODE;

/**
 * Payment method entity.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@FieldNameConstants
@Table(name = "payment_methods", schema = "household_schema")
public class PaymentMethod {

    private static final String DEFAULT_MASKED_IDENTIFIER = "***";
    private static final String CREDIT_CARD_MASK_PATTERN = ".*\\d{4}$";

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
    private String currency = DEFAULT_CURRENCY_CODE;

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

    /**
     * Validates payment method based on its type.
     *
     * @return true if valid
     */
    public boolean validate() {
        return !switch (methodType) {
            case CREDIT_CARD -> maskedIdentifier != null && maskedIdentifier.matches(CREDIT_CARD_MASK_PATTERN);
            case BANK_ACCOUNT -> maskedIdentifier != null && !maskedIdentifier.isBlank();
            default -> true;
        };
    }

    /**
     * Returns masked identifier for display.
     *
     * @return masked identifier
     */
    public String getMaskedIdentifier() {
        if (maskedIdentifier != null && !maskedIdentifier.isEmpty()) {
            return maskedIdentifier;
        }
        return DEFAULT_MASKED_IDENTIFIER;
    }

    /**
     * Returns payment provider with fallback.
     *
     * @return payment provider
     */
    public PaymentProvider getProvider() {
        return provider != null ? provider : PaymentProvider.SBERBANK;
    }
}